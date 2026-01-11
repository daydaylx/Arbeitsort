import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from '@modelcontextprotocol/sdk/types.js';

import * as fs from 'fs';
import * as path from 'path';

export type { Tool };

export interface McpServerContext {
  projectPath: string;
  config: Record<string, any>;
  logger: Logger;
}

export interface Logger {
  info(message: string, ...args: any[]): void;
  warn(message: string, ...args: any[]): void;
  error(message: string, ...args: any[]): void;
  debug(message: string, ...args: any[]): void;
}

export class ConsoleLogger implements Logger {
  private prefix: string;

  constructor(prefix: string) {
    this.prefix = prefix;
  }

  info(message: string, ...args: any[]): void {
    console.log(`[${this.prefix}] INFO:`, message, ...args);
  }

  warn(message: string, ...args: any[]): void {
    console.warn(`[${this.prefix}] WARN:`, message, ...args);
  }

  error(message: string, ...args: any[]): void {
    console.error(`[${this.prefix}] ERROR:`, message, ...args);
  }

  debug(message: string, ...args: any[]): void {
    if (process.env.DEBUG) {
      console.debug(`[${this.prefix}] DEBUG:`, message, ...args);
    }
  }
}

export abstract class McpServerBase {
  protected server: Server;
  protected context: McpServerContext;
  protected tools: Tool[] = [];

  constructor(name: string, version: string, context: McpServerContext) {
    this.context = context;
    this.server = new Server(
      {
        name,
        version,
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.setupRequestHandlers();
  }

  protected abstract registerTools(): void;

  private setupRequestHandlers(): void {
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: this.tools,
      };
    });

    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const toolName = request.params.name;
      const args = request.params.arguments || {};

      this.context.logger.debug(`Tool called: ${toolName}`, args);

      try {
        const result = await this.handleToolCall(toolName, args);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      } catch (error) {
        this.context.logger.error(`Tool error: ${toolName}`, error);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(
                {
                  error: error instanceof Error ? error.message : String(error),
                  tool: toolName,
                },
                null,
                2
              ),
            },
          ],
          isError: true,
        };
      }
    });
  }

  protected abstract handleToolCall(toolName: string, args: any): Promise<any>;

  protected readFile(filePath: string): string {
    const fullPath = path.join(this.context.projectPath, filePath);
    if (!fs.existsSync(fullPath)) {
      throw new Error(`File not found: ${filePath}`);
    }
    return fs.readFileSync(fullPath, 'utf-8');
  }

  protected writeFile(filePath: string, content: string): void {
    const fullPath = path.join(this.context.projectPath, filePath);
    const dir = path.dirname(fullPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(fullPath, content, 'utf-8');
  }

  protected fileExists(filePath: string): boolean {
    const fullPath = path.join(this.context.projectPath, filePath);
    return fs.existsSync(fullPath);
  }

  protected listFiles(dirPath: string, recursive: boolean = false): string[] {
    const fullPath = path.join(this.context.projectPath, dirPath);
    if (!fs.existsSync(fullPath)) {
      return [];
    }

    const files: string[] = [];
    const items = fs.readdirSync(fullPath);

    for (const item of items) {
      const itemPath = path.join(fullPath, item);
      const stat = fs.statSync(itemPath);
      const relativePath = path.join(dirPath, item);

      if (stat.isDirectory() && recursive) {
        files.push(...this.listFiles(relativePath, true));
      } else if (stat.isFile()) {
        files.push(relativePath);
      }
    }

    return files;
  }

  public async start(): Promise<void> {
    this.registerTools();
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    this.context.logger.info('Server started');
  }
}
