import { McpServerBase, McpServerContext } from '../base/McpServerBase.js';
import * as path from 'path';

interface ManifestIssue {
  type: 'security' | 'configuration' | 'permission';
  severity: 'critical' | 'warning' | 'info';
  message: string;
  line?: number;
}

export class ManifestValidatorServer extends McpServerBase {
  constructor(context: McpServerContext) {
    super('manifest-validator', '1.0.0', context);
  }

  protected registerTools(): void {
    this.tools = [
      {
        name: 'validate_manifest',
        description: 'Scan AndroidManifest.xml for security and configuration issues',
        inputSchema: {
          type: 'object',
          properties: {
            manifestPath: {
              type: 'string',
              description: 'Path to AndroidManifest.xml (default: app/src/main/AndroidManifest.xml)',
              default: 'app/src/main/AndroidManifest.xml',
            },
          },
        },
      },
    ];
  }

  protected async handleToolCall(toolName: string, args: any): Promise<any> {
    switch (toolName) {
      case 'validate_manifest':
        return this.validateManifest(args.manifestPath || 'app/src/main/AndroidManifest.xml');
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  private async validateManifest(manifestPath: string): Promise<any> {
    this.context.logger.info(`Validating manifest: ${manifestPath}`);
    
    if (!this.fileExists(manifestPath)) {
        throw new Error(`Manifest not found at ${manifestPath}`);
    }

    const content = this.readFile(manifestPath);
    const issues: ManifestIssue[] = [];

    // 1. Check for exported activities/receivers/services without permission
    // Regex matches <activity ... android:exported="true" ...> (simplified)
    const exportedRegex = /<(activity|service|receiver|provider)[^>]*android:exported\s*=\s*"true"[^>]*>/g;
    let match;
    while ((match = exportedRegex.exec(content)) !== null) {
        const tagContent = match[0];
        if (!tagContent.includes('android:permission')) {
             // Main launcher activity is an exception usually, but strictly it's "exported=true" so it's public.
             // We warn if it's NOT the main launcher (heuristic: main launcher has intent-filter MAIN/LAUNCHER)
             // But simpler regex here: warn generally for review.
             if (!tagContent.includes('android.intent.action.MAIN')) {
                 issues.push({
                     type: 'security',
                     severity: 'warning',
                     message: `Exported component found without explicit permission protection: ${this.extractName(tagContent)}`,
                 });
             }
        }
    }

    // 2. Check for dangerous permissions
    const dangerousPermissions = [
        'android.permission.READ_EXTERNAL_STORAGE',
        'android.permission.WRITE_EXTERNAL_STORAGE',
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.RECORD_AUDIO',
        'android.permission.CAMERA'
    ];

    for (const perm of dangerousPermissions) {
        if (content.includes(perm)) {
            issues.push({
                type: 'permission',
                severity: 'info',
                message: `Uses dangerous permission: ${perm}. Ensure runtime permission handling is implemented.`,
            });
        }
    }

    // 3. Check for debuggable
    if (content.includes('android:debuggable="true"')) {
        issues.push({
            type: 'security',
            severity: 'critical',
            message: 'Application is marked as debuggable="true". Do not release with this flag.',
        });
    }

    // 4. Check for allowBackup
    if (content.includes('android:allowBackup="true"')) {
         issues.push({
            type: 'security',
            severity: 'warning',
            message: 'android:allowBackup is set to true. Ensure sensitive data is excluded from backup.',
        });
    }

    return {
        file: manifestPath,
        issues,
        summary: {
            critical: issues.filter(i => i.severity === 'critical').length,
            warning: issues.filter(i => i.severity === 'warning').length,
            info: issues.filter(i => i.severity === 'info').length,
        }
    };
  }

  private extractName(tagContent: string): string {
      const match = tagContent.match(/android:name\s*=\s*"([^"]+)"/);
      return match ? match[1] : 'unknown component';
  }
}
