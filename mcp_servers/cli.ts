#!/usr/bin/env node

import { createServer, AVAILABLE_SERVERS } from './dist/index.js';
import * as path from 'path';
import * as fs from 'fs';

const PROJECT_ROOT = path.resolve(__dirname, '..');

function printUsage() {
  console.log('MontageZeit MCP Servers CLI');
  console.log('');
  console.log('Usage: node cli.js <server> <command> [options]');
  console.log('');
  console.log('Available servers:');
  for (const server of AVAILABLE_SERVERS) {
    console.log(`  ${server.name.padEnd(20)} - ${server.description}`);
  }
  console.log('');
  console.log('Commands:');
  console.log('  list-tools          - List all available tools for a server');
  console.log('  help                - Show help for a server');
  console.log('  run <tool> <args> - Run a specific tool');
  console.log('');
  console.log('Examples:');
  console.log('  node cli.js kotlin-reviewer list-tools');
  console.log('  node cli.js android-tester help');
  console.log('  node cli.js gradle-auditor run audit_dependencies');
  console.log('  node cli.js kotlin-reviewer run review_kotlin_file \'{"filePath":"app/src/main/java/de/montagezeit/app/MainActivity.kt"}\'');
}

function main() {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    printUsage();
    process.exit(1);
  }

  const serverName = args[0];
  const command = args[1];
  const toolArgs = args[2];

  // Check if server exists
  const serverExists = AVAILABLE_SERVERS.some(s => s.name === serverName);
  if (!serverExists) {
    console.error(`Error: Unknown server "${serverName}"`);
    console.log('');
    printUsage();
    process.exit(1);
  }

  try {
    const server = createServer(serverName, PROJECT_ROOT);

    switch (command) {
      case 'list-tools':
        console.log(JSON.stringify(server['tools'], null, 2));
        break;

      case 'help':
        console.log(`Server: ${serverName}`);
        console.log(`Description: ${AVAILABLE_SERVERS.find(s => s.name === serverName)?.description}`);
        console.log('');
        console.log('Available tools:');
        for (const tool of server['tools']) {
          console.log(`  ${tool.name}`);
          console.log(`    ${tool.description}`);
        }
        break;

      case 'run':
        if (!toolArgs) {
          console.error('Error: Tool name and arguments required for "run" command');
          console.log('Usage: node cli.js <server> run <tool> <args>');
          process.exit(1);
        }

        const toolName = toolArgs.split(' ')[0];
        const argsStr = toolArgs.substring(toolName.length + 1);
        const parsedArgs = argsStr ? JSON.parse(argsStr) : {};

        console.log(`Running tool: ${toolName}`);
        console.log(`Arguments:`, parsedArgs);
        console.log('');
        console.log('Note: This is a CLI wrapper. For full MCP functionality,');
        console.log('      run the server via stdio and use the MCP client.');
        console.log('');
        console.log('Example MCP client usage:');
        console.log(`  mcp call ${serverName} ${toolName} '${argsStr}'`);
        break;

      default:
        console.error(`Error: Unknown command "${command}"`);
        console.log('');
        printUsage();
        process.exit(1);
    }
  } catch (error) {
    console.error('Error:', error instanceof Error ? error.message : String(error));
    process.exit(1);
  }
}

main();
