import { McpServerBase, McpServerContext, Tool } from '../base/McpServerBase.js';
import * as fs from 'fs';
import * as path from 'path';

interface Dependency {
  group: string;
  name: string;
  version: string;
  file: string;
}

interface SecurityIssue {
  severity: 'critical' | 'high' | 'medium' | 'low';
  dependency: string;
  version: string;
  cve?: string;
  description: string;
  recommendation: string;
}

interface AuditResult {
  file: string;
  dependencies: Dependency[];
  securityIssues: SecurityIssue[];
  outdated: Dependency[];
  summary: {
    totalDependencies: number;
    securityIssues: number;
    outdatedCount: number;
  };
}

interface LicenseInfo {
  dependency: string;
  license: string;
  compliant: boolean;
}

export class GradleAuditorServer extends McpServerBase {
  private readonly KNOWN_VULNERABILITIES: Record<string, SecurityIssue[]> = {
    'androidx.core:core-ktx': [
      {
        severity: 'low',
        dependency: 'androidx.core:core-ktx',
        version: '<1.12.0',
        description: 'Older versions may have performance issues',
        recommendation: 'Update to 1.12.0 or later',
      },
    ],
    'androidx.room:room-runtime': [
      {
        severity: 'medium',
        dependency: 'androidx.room:room-runtime',
        version: '<2.6.0',
        description: 'Older Room versions have known bugs',
        recommendation: 'Update to 2.6.1 or later',
      },
    ],
    'com.google.android.gms:play-services-location': [
      {
        severity: 'medium',
        dependency: 'com.google.android.gms:play-services-location',
        version: '<21.0.0',
        description: 'Older versions may have location accuracy issues',
        recommendation: 'Update to 21.0.1 or later',
      },
    ],
  };

  private readonly OUTDATED_VERSIONS: Record<string, string> = {
    'androidx.core:core-ktx': '1.12.0',
    'androidx.lifecycle:lifecycle-runtime-ktx': '2.6.2',
    'androidx.activity:activity-compose': '1.8.1',
    'androidx.room:room-runtime': '2.6.1',
    'androidx.room:room-ktx': '2.6.1',
    'androidx.datastore:datastore-preferences': '1.0.0',
    'androidx.work:work-runtime-ktx': '2.9.0',
    'com.google.android.gms:play-services-location': '21.0.1',
    'org.jetbrains.kotlinx:kotlinx-coroutines-android': '1.7.3',
    'com.google.dagger:hilt-android': '2.48',
  };

  constructor(context: McpServerContext) {
    super('gradle-auditor', '1.0.0', context);
  }

  protected registerTools(): void {
    this.tools = [
      {
        name: 'audit_dependencies',
        description: 'Audit all Gradle dependencies for security vulnerabilities and outdated versions',
        inputSchema: {
          type: 'object',
          properties: {
            checkSecurity: {
              type: 'boolean',
              description: 'Check for security vulnerabilities',
              default: true,
            },
            checkOutdated: {
              type: 'boolean',
              description: 'Check for outdated dependencies',
              default: true,
            },
          },
        },
      },
      {
        name: 'check_gradle_file',
        description: 'Analyze a specific Gradle build file',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to Gradle file (build.gradle.kts or settings.gradle.kts)',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'check_licenses',
        description: 'Check dependency licenses for compliance',
        inputSchema: {
          type: 'object',
          properties: {
            allowedLicenses: {
              type: 'array',
              items: { type: 'string' },
              description: 'List of allowed license types',
              default: ['Apache-2.0', 'MIT', 'BSD-3-Clause'],
            },
          },
        },
      },
      {
        name: 'suggest_updates',
        description: 'Suggest dependency updates based on current versions',
        inputSchema: {
          type: 'object',
          properties: {
            includePatch: {
              type: 'boolean',
              description: 'Include patch updates',
              default: true,
            },
            includeMinor: {
              type: 'boolean',
              description: 'Include minor updates',
              default: true,
            },
            includeMajor: {
              type: 'boolean',
              description: 'Include major updates',
              default: false,
            },
          },
        },
      },
      {
        name: 'analyze_dependency_graph',
        description: 'Analyze dependency graph for potential conflicts',
        inputSchema: {
          type: 'object',
          properties: {},
        },
      },
    ];
  }

  protected async handleToolCall(toolName: string, args: any): Promise<any> {
    switch (toolName) {
      case 'audit_dependencies':
        return this.auditDependencies(args.checkSecurity, args.checkOutdated);
      case 'check_gradle_file':
        return this.checkGradleFile(args.filePath);
      case 'check_licenses':
        return this.checkLicenses(args.allowedLicenses);
      case 'suggest_updates':
        return this.suggestUpdates(args.includePatch, args.includeMinor, args.includeMajor);
      case 'analyze_dependency_graph':
        return this.analyzeDependencyGraph();
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  private async auditDependencies(checkSecurity: boolean, checkOutdated: boolean): Promise<AuditResult[]> {
    this.context.logger.info('Auditing all Gradle dependencies');

    const gradleFiles = this.listFiles('.', false).filter(f => 
      f.endsWith('build.gradle.kts') || f.endsWith('settings.gradle.kts')
    );

    const results: AuditResult[] = [];

    for (const file of gradleFiles) {
      const result = await this.checkGradleFile(file);
      if (checkSecurity) {
        result.securityIssues = this.checkSecurityVulnerabilities(result.dependencies);
      }
      if (checkOutdated) {
        result.outdated = this.checkOutdatedDependencies(result.dependencies);
      }
      result.summary = {
        totalDependencies: result.dependencies.length,
        securityIssues: result.securityIssues.length,
        outdatedCount: result.outdated.length,
      };
      results.push(result);
    }

    return results;
  }

  private async checkGradleFile(filePath: string): Promise<AuditResult> {
    this.context.logger.info(`Checking Gradle file: ${filePath}`);

    const content = this.readFile(filePath);
    const dependencies = this.extractDependencies(content, filePath);

    return {
      file: filePath,
      dependencies,
      securityIssues: [],
      outdated: [],
      summary: {
        totalDependencies: dependencies.length,
        securityIssues: 0,
        outdatedCount: 0,
      },
    };
  }

  private async checkLicenses(allowedLicenses: string[]): Promise<LicenseInfo[]> {
    this.context.logger.info('Checking dependency licenses');

    const gradleFiles = this.listFiles('.', false).filter(f => 
      f.endsWith('build.gradle.kts')
    );

    const licenseInfos: LicenseInfo[] = [];

    for (const file of gradleFiles) {
      const content = this.readFile(file);
      const dependencies = this.extractDependencies(content, file);

      for (const dep of dependencies) {
        const license = this.getLicenseInfo(dep);
        const compliant = allowedLicenses.includes(license);

        licenseInfos.push({
          dependency: `${dep.group}:${dep.name}`,
          license,
          compliant,
        });
      }
    }

    return licenseInfos;
  }

  private async suggestUpdates(includePatch: boolean, includeMinor: boolean, includeMajor: boolean): Promise<any> {
    this.context.logger.info('Suggesting dependency updates');

    const gradleFiles = this.listFiles('.', false).filter(f => 
      f.endsWith('build.gradle.kts')
    );

    const suggestions: any[] = [];

    for (const file of gradleFiles) {
      const content = this.readFile(file);
      const dependencies = this.extractDependencies(content, file);

      for (const dep of dependencies) {
        const latestVersion = this.OUTDATED_VERSIONS[`${dep.group}:${dep.name}`];
        if (latestVersion && this.isUpdateNeeded(dep.version, latestVersion, includePatch, includeMinor, includeMajor)) {
          suggestions.push({
            dependency: `${dep.group}:${dep.name}`,
            currentVersion: dep.version,
            suggestedVersion: latestVersion,
            updateType: this.getUpdateType(dep.version, latestVersion),
            file,
          });
        }
      }
    }

    return {
      suggestions,
      summary: {
        total: suggestions.length,
        patch: suggestions.filter(s => s.updateType === 'patch').length,
        minor: suggestions.filter(s => s.updateType === 'minor').length,
        major: suggestions.filter(s => s.updateType === 'major').length,
      },
    };
  }

  private async analyzeDependencyGraph(): Promise<any> {
    this.context.logger.info('Analyzing dependency graph');

    const gradleFiles = this.listFiles('.', false).filter(f => 
      f.endsWith('build.gradle.kts')
    );

    const graph: any = {
      nodes: [] as any[],
      edges: [] as any[],
    };

    for (const file of gradleFiles) {
      const content = this.readFile(file);
      const dependencies = this.extractDependencies(content, file);

      for (const dep of dependencies) {
        graph.nodes.push({
          id: `${dep.group}:${dep.name}`,
          version: dep.version,
          file,
        });

        // Add edges for common Android dependencies
        if (dep.group.startsWith('androidx.')) {
          graph.edges.push({
            from: 'androidx',
            to: `${dep.group}:${dep.name}`,
          });
        } else if (dep.group.startsWith('com.google.android.gms')) {
          graph.edges.push({
            from: 'google-play-services',
            to: `${dep.group}:${dep.name}`,
          });
        }
      }
    }

    return {
      graph,
      summary: {
        totalNodes: graph.nodes.length,
        totalEdges: graph.edges.length,
        potentialConflicts: this.detectPotentialConflicts(graph),
      },
    };
  }

  private extractDependencies(content: string, filePath: string): Dependency[] {
    const dependencies: Dependency[] = [];
    const lines = content.split('\n');

    for (const line of lines) {
      // Match implementation and testImplementation dependencies
      const match = line.match(/(?:implementation|testImplementation|androidTestImplementation)\s*\(\s*["']([^:"']+)["']\s*:\s*["']([^:"']+)["']\s*:\s*["']([^:"']+)["']/);
      if (match) {
        dependencies.push({
          group: match[1],
          name: match[2],
          version: match[3],
          file: filePath,
        });
      }

      // Match BOM dependencies
      const bomMatch = line.match(/platform\s*\(\s*["']([^:"']+)["']\s*:\s*["']([^:"']+)["']/);
      if (bomMatch) {
        dependencies.push({
          group: bomMatch[1],
          name: 'compose-bom',
          version: bomMatch[2],
          file: filePath,
        });
      }
    }

    return dependencies;
  }

  private checkSecurityVulnerabilities(dependencies: Dependency[]): SecurityIssue[] {
    const issues: SecurityIssue[] = [];

    for (const dep of dependencies) {
      const depKey = `${dep.group}:${dep.name}`;
      const vulns = this.KNOWN_VULNERABILITIES[depKey];

      if (vulns) {
        for (const vuln of vulns) {
          if (this.versionMatches(dep.version, vuln.version)) {
            issues.push({
              ...vuln,
              dependency: depKey,
              version: dep.version,
            });
          }
        }
      }
    }

    return issues;
  }

  private checkOutdatedDependencies(dependencies: Dependency[]): Dependency[] {
    const outdated: Dependency[] = [];

    for (const dep of dependencies) {
      const depKey = `${dep.group}:${dep.name}`;
      const latestVersion = this.OUTDATED_VERSIONS[depKey];

      if (latestVersion && this.isVersionOlder(dep.version, latestVersion)) {
        outdated.push({
          ...dep,
          version: latestVersion,
        });
      }
    }

    return outdated;
  }

  private getLicenseInfo(dep: Dependency): string {
    // Simplified license detection - in production would use API
    const licenseMap: Record<string, string> = {
      'androidx.': 'Apache-2.0',
      'com.google.android.gms': 'Apache-2.0',
      'com.google.dagger': 'Apache-2.0',
      'org.jetbrains.kotlinx': 'Apache-2.0',
      'io.mockk': 'Apache-2.0',
      'junit': 'Eclipse Public License 1.0',
    };

    for (const [prefix, license] of Object.entries(licenseMap)) {
      if (dep.group.startsWith(prefix)) {
        return license;
      }
    }

    return 'Unknown';
  }

  private versionMatches(current: string, constraint: string): boolean {
    if (constraint.startsWith('<')) {
      const maxVersion = constraint.substring(1);
      return this.isVersionOlder(current, maxVersion);
    }
    return current === constraint;
  }

  private isVersionOlder(current: string, latest: string): boolean {
    const currentParts = current.split('.').map(Number);
    const latestParts = latest.split('.').map(Number);

    for (let i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
      const currentPart = currentParts[i] || 0;
      const latestPart = latestParts[i] || 0;

      if (currentPart < latestPart) {
        return true;
      } else if (currentPart > latestPart) {
        return false;
      }
    }

    return false;
  }

  private isUpdateNeeded(current: string, latest: string, includePatch: boolean, includeMinor: boolean, includeMajor: boolean): boolean {
    const currentParts = current.split('.').map(Number);
    const latestParts = latest.split('.').map(Number);

    const majorDiff = latestParts[0] - currentParts[0];
    const minorDiff = (latestParts[1] || 0) - (currentParts[1] || 0);
    const patchDiff = (latestParts[2] || 0) - (currentParts[2] || 0);

    if (majorDiff > 0 && includeMajor) return true;
    if (minorDiff > 0 && includeMinor) return true;
    if (patchDiff > 0 && includePatch) return true;

    return false;
  }

  private getUpdateType(current: string, latest: string): string {
    const currentParts = current.split('.').map(Number);
    const latestParts = latest.split('.').map(Number);

    if (latestParts[0] > currentParts[0]) return 'major';
    if (latestParts[1] > (currentParts[1] || 0)) return 'minor';
    return 'patch';
  }

  private detectPotentialConflicts(graph: any): string[] {
    const conflicts: string[] = [];
    const nodeMap = new Map<string, any>();

    for (const node of graph.nodes) {
      const existing = nodeMap.get(node.id);
      if (existing) {
        conflicts.push(`Duplicate dependency: ${node.id} (${existing.file} vs ${node.file})`);
      }
      nodeMap.set(node.id, node);
    }

    return conflicts;
  }
}
