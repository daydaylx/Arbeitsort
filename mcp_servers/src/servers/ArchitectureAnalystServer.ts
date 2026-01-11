import { McpServerBase, McpServerContext } from '../base/McpServerBase.js';
import * as path from 'path';

interface Violation {
  file: string;
  type: 'layer_violation' | 'circular_dependency' | 'package_structure';
  message: string;
  severity: 'error' | 'warning';
}

interface ArchitectureReport {
  timestamp: string;
  violations: Violation[];
  summary: {
    totalFiles: number;
    violationCount: number;
  };
}

export class ArchitectureAnalystServer extends McpServerBase {
  // Simple Clean Architecture rules: key imports shouldn't appear in value packages
  // e.g. Domain should not import Presentation or Data (usually)
  private readonly LAYER_RULES: Record<string, string[]> = {
    'domain': ['presentation', 'ui', 'data', 'framework'],
    'data': ['presentation', 'ui'],
    // Presentation/UI can usually import Domain
  };

  constructor(context: McpServerContext) {
    super('architecture-analyst', '1.0.0', context);
  }

  protected registerTools(): void {
    this.tools = [
      {
        name: 'analyze_architecture',
        description: 'Analyze the project for Clean Architecture violations and structure issues',
        inputSchema: {
          type: 'object',
          properties: {
            sourcePath: {
              type: 'string',
              description: 'Root path of source code (default: app/src/main/java)',
              default: 'app/src/main/java',
            },
          },
        },
      },
      {
        name: 'check_package_structure',
        description: 'List and validate the package structure of the project',
        inputSchema: {
          type: 'object',
          properties: {
             sourcePath: {
              type: 'string',
              description: 'Root path of source code (default: app/src/main/java)',
              default: 'app/src/main/java',
            },
          },
        },
      }
    ];
  }

  protected async handleToolCall(toolName: string, args: any): Promise<any> {
    switch (toolName) {
      case 'analyze_architecture':
        return this.analyzeArchitecture(args.sourcePath || 'app/src/main/java');
      case 'check_package_structure':
        return this.checkPackageStructure(args.sourcePath || 'app/src/main/java');
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  private async analyzeArchitecture(sourcePath: string): Promise<ArchitectureReport> {
    this.context.logger.info(`Analyzing architecture in ${sourcePath}`);
    const files = this.listFiles(sourcePath, true).filter(f => f.endsWith('.kt'));
    const violations: Violation[] = [];

    for (const file of files) {
      const content = this.readFile(path.join(sourcePath, file));
      const packageMatch = content.match(/package\s+([\w\.]+)/);
      if (!packageMatch) continue;

      const packageName = packageMatch[1];
      const imports = this.extractImports(content);

      // Determine current layer
      let currentLayer = 'unknown';
      if (packageName.includes('.domain')) currentLayer = 'domain';
      else if (packageName.includes('.data')) currentLayer = 'data';
      else if (packageName.includes('.presentation') || packageName.includes('.ui')) currentLayer = 'presentation';

      // Check layer rules
      if (currentLayer in this.LAYER_RULES) {
        const forbiddenLayers = this.LAYER_RULES[currentLayer];
        for (const imp of imports) {
          for (const forbidden of forbiddenLayers) {
            if (imp.includes(`.${forbidden}.`)) {
              violations.push({
                file: path.join(sourcePath, file),
                type: 'layer_violation',
                message: `Layer '${currentLayer}' should not import '${forbidden}': ${imp}`,
                severity: 'error'
              });
            }
          }
        }
      }
    }

    return {
      timestamp: new Date().toISOString(),
      violations,
      summary: {
        totalFiles: files.length,
        violationCount: violations.length
      }
    };
  }

  private async checkPackageStructure(sourcePath: string): Promise<any> {
    const files = this.listFiles(sourcePath, true).filter(f => f.endsWith('.kt'));
    const packages = new Set<string>();

    for (const file of files) {
        const content = this.readFile(path.join(sourcePath, file));
        const match = content.match(/package\s+([\w\.]+)/);
        if (match) packages.add(match[1]);
    }

    const packageList = Array.from(packages).sort();
    
    // Check for common root
    const commonRoot = this.findCommonRoot(packageList);
    
    return {
        rootPackage: commonRoot,
        packages: packageList,
        totalPackages: packageList.length
    };
  }

  private extractImports(content: string): string[] {
    const imports: string[] = [];
    const lines = content.split('\n');
    for (const line of lines) {
      const match = line.match(/^\s*import\s+([\w\.]+)/);
      if (match) imports.push(match[1]);
    }
    return imports;
  }

  private findCommonRoot(packages: string[]): string {
      if (packages.length === 0) return '';
      let prefix = packages[0];
      for (let i = 1; i < packages.length; i++) {
          while (packages[i].indexOf(prefix) !== 0) {
              prefix = prefix.substring(0, prefix.lastIndexOf('.'));
              if (prefix === "") return "";
          }
      }
      return prefix;
  }
}
