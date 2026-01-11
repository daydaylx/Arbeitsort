import { McpServerBase, McpServerContext } from '../base/McpServerBase.js';
import * as path from 'path';

interface ResourceIssue {
  type: 'unused_resource' | 'missing_translation';
  id: string;
  message: string;
  file?: string;
}

export class ResourceOptimizerServer extends McpServerBase {
  constructor(context: McpServerContext) {
    super('resource-optimizer', '1.0.0', context);
  }

  protected registerTools(): void {
    this.tools = [
      {
        name: 'find_unused_resources',
        description: 'Find potentially unused resources (strings, colors, dimens)',
        inputSchema: {
          type: 'object',
          properties: {
             resPath: {
              type: 'string',
              description: 'Path to res directory (default: app/src/main/res)',
              default: 'app/src/main/res',
            },
          },
        },
      },
      {
        name: 'check_missing_translations',
        description: 'Check for string keys missing in other languages',
        inputSchema: {
          type: 'object',
          properties: {
             resPath: {
              type: 'string',
              description: 'Path to res directory (default: app/src/main/res)',
              default: 'app/src/main/res',
            },
          },
        },
      }
    ];
  }

  protected async handleToolCall(toolName: string, args: any): Promise<any> {
    const resPath = args.resPath || 'app/src/main/res';
    switch (toolName) {
      case 'find_unused_resources':
        return this.findUnusedResources(resPath);
      case 'check_missing_translations':
        return this.checkMissingTranslations(resPath);
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  private async findUnusedResources(resPath: string): Promise<any> {
    this.context.logger.info(`Analyzing resources in ${resPath}`);
    
    // 1. Collect all defined resources from values/strings.xml, colors.xml, dimens.xml
    const definedResources = await this.collectDefinedResources(resPath); // Map<Type, Set<Name>>
    
    // 2. Scan all code and XML layouts for usages
    const sourcePath = path.join(path.dirname(path.dirname(resPath)), 'java'); // ../java from res
    const usageCounts = await this.scanForUsages(sourcePath, resPath, definedResources);

    const unused: ResourceIssue[] = [];
    
    for (const [type, names] of definedResources.entries()) {
        for (const name of names) {
            // Check if used in code (R.type.name) or xml (@type/name)
            // Simplified check: just search for the name string. 
            // Note: This produces false negatives if name is very generic (e.g. "id"), but safer than false positives.
            // Actually, for "unused", false positives are annoying. We need to be careful.
            // Let's rely on the scanForUsages to be reasonably smart.
            if (!usageCounts.has(`${type}:${name}`)) {
                unused.push({
                    type: 'unused_resource',
                    id: `@${type}/${name}`,
                    message: `Potentially unused resource: @${type}/${name}`,
                });
            }
        }
    }

    return {
        totalDefined: Array.from(definedResources.values()).reduce((a, b) => a + b.size, 0),
        unusedCount: unused.length,
        unusedResources: unused
    };
  }

  private async collectDefinedResources(resPath: string): Promise<Map<string, Set<string>>> {
      const resources = new Map<string, Set<string>>();
      const valuesPath = path.join(resPath, 'values');
      
      if (!this.fileExists(valuesPath)) return resources;

      const files = this.listFiles(path.relative(this.context.projectPath, valuesPath));
      
      for (const file of files) {
          if (!file.endsWith('.xml')) continue;
          const content = this.readFile(path.join(path.relative(this.context.projectPath, valuesPath), file));
          
          // Match <string name="foo">, <color name="bar">, <dimen name="baz">
          const regex = /<(string|color|dimen)\s+name\s*=\s*"([^"]+)"/g;
          let match;
          while ((match = regex.exec(content)) !== null) {
              const type = match[1];
              const name = match[2];
              if (!resources.has(type)) resources.set(type, new Set());
              resources.get(type)!.add(name);
          }
      }
      return resources;
  }

  private async scanForUsages(sourceDir: string, resDir: string, defined: Map<string, Set<string>>): Promise<Set<string>> {
      const found = new Set<string>();
      
      // Get all files to scan
      const javaFiles = this.fileExists(sourceDir) ? this.listFiles(path.relative(this.context.projectPath, sourceDir), true) : [];
      const resFiles = this.fileExists(resDir) ? this.listFiles(path.relative(this.context.projectPath, resDir), true) : [];
      
      const allFiles = [...javaFiles.map(f => path.join(sourceDir, f)), ...resFiles.map(f => path.join(resDir, f))];

      for (const file of allFiles) {
          const content = this.readFile(file);
          
          for (const [type, names] of defined.entries()) {
              for (const name of names) {
                  const key = `${type}:${name}`;
                  if (found.has(key)) continue;

                  // Java/Kotlin: R.string.name
                  if (content.includes(`R.${type}.${name}`)) {
                      found.add(key);
                      continue;
                  }
                  // XML: @string/name
                  if (content.includes(`@${type}/${name}`)) {
                       found.add(key);
                       continue;
                  }
                  // Compose: stringResource(R.string.name) -> covered by R.string.name
              }
          }
      }
      return found;
  }

  private async checkMissingTranslations(resPath: string): Promise<any> {
      // Find base values/strings.xml
      const baseStringsPath = path.join(resPath, 'values', 'strings.xml');
      if (!this.fileExists(baseStringsPath)) return { error: 'Base strings.xml not found' };

      const baseKeys = this.extractStringKeys(this.readFile(baseStringsPath));
      
      // Find other values-*/strings.xml
      const resDirItems = this.listFiles(path.relative(this.context.projectPath, resPath));
      const issues: ResourceIssue[] = [];

      for (const item of resDirItems) {
          if (item.startsWith('values-') && this.fileExists(path.join(resPath, item, 'strings.xml'))) {
               const langContent = this.readFile(path.join(resPath, item, 'strings.xml'));
               const langKeys = this.extractStringKeys(langContent);
               
               for (const key of baseKeys) {
                   if (!langKeys.has(key)) {
                       issues.push({
                           type: 'missing_translation',
                           id: key,
                           message: `Missing translation for '${key}' in ${item}`,
                           file: path.join(resPath, item, 'strings.xml')
                       });
                   }
               }
          }
      }

      return {
          totalBaseKeys: baseKeys.size,
          missingCount: issues.length,
          issues
      };
  }

  private extractStringKeys(content: string): Set<string> {
      const keys = new Set<string>();
      const regex = /<string\s+name\s*=\s*"([^"]+)"/g;
      let match;
      while ((match = regex.exec(content)) !== null) {
          // Ignore translatable="false"
          // We need to check the full tag for translatable="false"
          // This is a simplified check.
           const fullTagMatch = content.substring(match.index).match(/^<string[^>]*>/);
           if (fullTagMatch && fullTagMatch[0].includes('translatable="false"')) {
               continue;
           }
          keys.add(match[1]);
      }
      return keys;
  }
}
