import { McpServerBase, McpServerContext, Tool } from '../base/McpServerBase.js';
import * as fs from 'fs';
import * as path from 'path';

interface ReviewIssue {
  severity: 'error' | 'warning' | 'info';
  file: string;
  line: number;
  column: number;
  message: string;
  rule: string;
  suggestion?: string;
}

interface ReviewResult {
  file: string;
  issues: ReviewIssue[];
  summary: {
    total: number;
    errors: number;
    warnings: number;
    info: number;
  };
}

export class KotlinReviewerServer extends McpServerBase {
  constructor(context: McpServerContext) {
    super('kotlin-reviewer', '1.0.0', context);
  }

  protected registerTools(): void {
    this.tools = [
      {
        name: 'review_kotlin_file',
        description: 'Review a Kotlin file for Android best practices, Compose patterns, and Clean Architecture violations',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to the Kotlin file to review (relative to project root)',
            },
            focus: {
              type: 'string',
              enum: ['all', 'compose', 'architecture', 'security', 'performance'],
              description: 'Focus area for the review',
              default: 'all',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'review_kotlin_directory',
        description: 'Review all Kotlin files in a directory',
        inputSchema: {
          type: 'object',
          properties: {
            dirPath: {
              type: 'string',
              description: 'Path to the directory to review (relative to project root)',
            },
            recursive: {
              type: 'boolean',
              description: 'Review files recursively',
              default: true,
            },
            focus: {
              type: 'string',
              enum: ['all', 'compose', 'architecture', 'security', 'performance'],
              description: 'Focus area for the review',
              default: 'all',
            },
          },
          required: ['dirPath'],
        },
      },
      {
        name: 'check_compose_patterns',
        description: 'Check Jetpack Compose code for best practices and anti-patterns',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to the Compose file to check',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'check_clean_architecture',
        description: 'Verify Clean Architecture principles in the codebase',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to the file to check',
            },
          },
          required: ['filePath'],
        },
      },
    ];
  }

  protected async handleToolCall(toolName: string, args: any): Promise<any> {
    switch (toolName) {
      case 'review_kotlin_file':
        return this.reviewFile(args.filePath, args.focus);
      case 'review_kotlin_directory':
        return this.reviewDirectory(args.dirPath, args.recursive, args.focus);
      case 'check_compose_patterns':
        return this.checkComposePatterns(args.filePath);
      case 'check_clean_architecture':
        return this.checkCleanArchitecture(args.filePath);
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  private async reviewFile(filePath: string, focus: string = 'all'): Promise<ReviewResult> {
    this.context.logger.info(`Reviewing file: ${filePath} with focus: ${focus}`);

    const content = this.readFile(filePath);
    const issues: ReviewIssue[] = [];

    // General Kotlin checks
    if (focus === 'all' || focus === 'security') {
      issues.push(...this.checkSecurityIssues(content, filePath));
    }

    if (focus === 'all' || focus === 'performance') {
      issues.push(...this.checkPerformanceIssues(content, filePath));
    }

    // Compose-specific checks
    if (focus === 'all' || focus === 'compose') {
      if (filePath.includes('ui/screen') || filePath.includes('ui/theme')) {
        issues.push(...this.checkComposeIssues(content, filePath));
      }
    }

    // Clean Architecture checks
    if (focus === 'all' || focus === 'architecture') {
      issues.push(...this.checkArchitectureIssues(content, filePath));
    }

    const summary = {
      total: issues.length,
      errors: issues.filter(i => i.severity === 'error').length,
      warnings: issues.filter(i => i.severity === 'warning').length,
      info: issues.filter(i => i.severity === 'info').length,
    };

    return { file: filePath, issues, summary };
  }

  private async reviewDirectory(dirPath: string, recursive: boolean, focus: string): Promise<ReviewResult[]> {
    this.context.logger.info(`Reviewing directory: ${dirPath}`);

    const files = this.listFiles(dirPath, recursive).filter(f => f.endsWith('.kt'));
    const results: ReviewResult[] = [];

    for (const file of files) {
      const result = await this.reviewFile(file, focus);
      results.push(result);
    }

    return results;
  }

  private checkSecurityIssues(content: string, filePath: string): ReviewIssue[] {
    const issues: ReviewIssue[] = [];
    const lines = content.split('\n');

    lines.forEach((line, index) => {
      // Check for hardcoded API keys or secrets
      if (line.match(/api[_-]?key\s*=\s*["'][^"']+["']/i)) {
        issues.push({
          severity: 'error',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Hardcoded API key detected',
          rule: 'SEC001',
          suggestion: 'Move API keys to local.properties or secure storage',
        });
      }

      // Check for hardcoded passwords
      if (line.match(/password\s*=\s*["'][^"']+["']/i)) {
        issues.push({
          severity: 'error',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Hardcoded password detected',
          rule: 'SEC002',
          suggestion: 'Use secure storage for credentials',
        });
      }

      // Check for logging sensitive data
      if (line.match(/Log\.[dewi]\(.*(?:password|token|secret|credential)/i)) {
        issues.push({
          severity: 'warning',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Logging sensitive data',
          rule: 'SEC003',
          suggestion: 'Remove sensitive data from logs',
        });
      }
    });

    return issues;
  }

  private checkPerformanceIssues(content: string, filePath: string): ReviewIssue[] {
    const issues: ReviewIssue[] = [];
    const lines = content.split('\n');

    lines.forEach((line, index) => {
      // Check for expensive operations in Composable
      if (line.includes('@Composable') && line.match(/\.collect\(\)/)) {
        issues.push({
          severity: 'warning',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Using collect() in Composable can cause recomposition',
          rule: 'PERF001',
          suggestion: 'Use collectAsState() or derivedStateOf() instead',
        });
      }

      // Check for unnecessary object allocations
      if (line.match(/new\s+\w+\(\)/) && !line.match(/val\s+\w+\s*=/)) {
        issues.push({
          severity: 'info',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Potential unnecessary object allocation',
          rule: 'PERF002',
          suggestion: 'Consider reusing objects or using object pools',
        });
      }
    });

    return issues;
  }

  private checkComposeIssues(content: string, filePath: string): ReviewIssue[] {
    const issues: ReviewIssue[] = [];
    const lines = content.split('\n');

    lines.forEach((line, index) => {
      // Check for remember without key
      if (line.includes('remember {') && !line.includes('key =')) {
        issues.push({
          severity: 'warning',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'remember without key may cause unexpected behavior',
          rule: 'COMP001',
          suggestion: 'Add a key parameter to remember() for list items',
        });
      }

      // Check for mutable state in Composable
      if (line.includes('var ') && line.includes('mutableStateOf')) {
        issues.push({
          severity: 'warning',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Using var with mutableStateOf is redundant',
          rule: 'COMP002',
          suggestion: 'Use val with mutableStateOf',
        });
      }

      // Check for missing @Preview annotation
      if (line.includes('@Composable') && !line.includes('fun ') && !line.includes('Preview')) {
        const nextLine = lines[index + 1];
        if (nextLine && nextLine.includes('fun ') && !nextLine.includes('Preview')) {
          issues.push({
            severity: 'info',
            file: filePath,
            line: index + 2,
            column: 0,
            message: 'Composable function without @Preview',
            rule: 'COMP003',
            suggestion: 'Add @Preview annotation for better development experience',
          });
        }
      }
    });

    return issues;
  }

  private checkArchitectureIssues(content: string, filePath: string): ReviewIssue[] {
    const issues: ReviewIssue[] = [];
    const lines = content.split('\n');

    // Check layer violations
    const isDataLayer = filePath.includes('/data/');
    const isDomainLayer = filePath.includes('/domain/');
    const isUiLayer = filePath.includes('/ui/');

    lines.forEach((line, index) => {
      // Data layer should not import UI
      if (isDataLayer && line.match(/import.*de\.montagezeit\.app\.ui\./)) {
        issues.push({
          severity: 'error',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Data layer importing UI components - architecture violation',
          rule: 'ARCH001',
          suggestion: 'Move UI dependencies to appropriate layer',
        });
      }

      // Domain layer should not import Android framework
      if (isDomainLayer && line.match(/import android\./)) {
        issues.push({
          severity: 'error',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'Domain layer importing Android framework - architecture violation',
          rule: 'ARCH002',
          suggestion: 'Use interfaces and abstractions instead',
        });
      }

      // UI layer should not directly access database
      if (isUiLayer && line.match(/WorkEntryDao|AppDatabase/)) {
        issues.push({
          severity: 'error',
          file: filePath,
          line: index + 1,
          column: 0,
          message: 'UI layer directly accessing database - architecture violation',
          rule: 'ARCH003',
          suggestion: 'Use UseCases for data access',
        });
      }
    });

    return issues;
  }

  private async checkComposePatterns(filePath: string): Promise<any> {
    this.context.logger.info(`Checking Compose patterns: ${filePath}`);
    const content = this.readFile(filePath);
    const issues = this.checkComposeIssues(content, filePath);

    return {
      file: filePath,
      issues,
      summary: {
        total: issues.length,
        errors: issues.filter(i => i.severity === 'error').length,
        warnings: issues.filter(i => i.severity === 'warning').length,
        info: issues.filter(i => i.severity === 'info').length,
      },
    };
  }

  private async checkCleanArchitecture(filePath: string): Promise<any> {
    this.context.logger.info(`Checking Clean Architecture: ${filePath}`);
    const content = this.readFile(filePath);
    const issues = this.checkArchitectureIssues(content, filePath);

    return {
      file: filePath,
      issues,
      summary: {
        total: issues.length,
        errors: issues.filter(i => i.severity === 'error').length,
        warnings: issues.filter(i => i.severity === 'warning').length,
        info: issues.filter(i => i.severity === 'info').length,
      },
    };
  }
}
