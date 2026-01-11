import { McpServerContext, ConsoleLogger } from './base/McpServerBase.js';
import { KotlinReviewerServer } from './servers/KotlinReviewerServer.js';
import { AndroidTesterServer } from './servers/AndroidTesterServer.js';
import { GradleAuditorServer } from './servers/GradleAuditorServer.js';
import { ArchitectureAnalystServer } from './servers/ArchitectureAnalystServer.js';
import { ManifestValidatorServer } from './servers/ManifestValidatorServer.js';
import { ResourceOptimizerServer } from './servers/ResourceOptimizerServer.js';

export function createServer(serverName: string, projectPath: string): any {
  const context: McpServerContext = {
    projectPath,
    config: {},
    logger: new ConsoleLogger(serverName),
  };

  switch (serverName) {
    case 'kotlin-reviewer':
      return new KotlinReviewerServer(context);
    case 'android-tester':
      return new AndroidTesterServer(context);
    case 'gradle-auditor':
      return new GradleAuditorServer(context);
    case 'architecture-analyst':
      return new ArchitectureAnalystServer(context);
    case 'manifest-validator':
      return new ManifestValidatorServer(context);
    case 'resource-optimizer':
      return new ResourceOptimizerServer(context);
    default:
      throw new Error(`Unknown server: ${serverName}`);
  }
}

export const AVAILABLE_SERVERS = [
  {
    name: 'kotlin-reviewer',
    description: 'Review Kotlin code for Android best practices, Compose patterns, and Clean Architecture',
  },
  {
    name: 'android-tester',
    description: 'Generate unit and instrumented tests for Android components',
  },
  {
    name: 'gradle-auditor',
    description: 'Audit Gradle dependencies for security vulnerabilities and outdated versions',
  },
  {
    name: 'architecture-analyst',
    description: 'Analyze Clean Architecture violations and package structure',
  },
  {
    name: 'manifest-validator',
    description: 'Validate AndroidManifest.xml for security and configuration',
  },
  {
    name: 'resource-optimizer',
    description: 'Find unused resources and missing translations',
  },
];
