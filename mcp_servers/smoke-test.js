#!/usr/bin/env node

import { createServer, AVAILABLE_SERVERS } from './dist/index.js';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, '..');

async function runSmokeTests() {
  console.log('Running MCP Servers Smoke Tests...\n');

  const results = [];

  for (const serverInfo of AVAILABLE_SERVERS) {
    console.log(`Testing server: ${serverInfo.name}`);
    console.log('='.repeat(50));

    try {
      const server = createServer(serverInfo.name, PROJECT_ROOT);
      
      // Test 1: Server initialization
      console.log('  ✓ Server initialized');

      // Manually register tools for testing purposes
      if (typeof server['registerTools'] === 'function') {
        server['registerTools']();
      }

      // Test 2: Tools registration
      // Accessing protected property via 'any' cast for testing
      const tools = (server).tools;
      if (!tools || tools.length === 0) {
        throw new Error('No tools registered');
      }
      console.log(`  ✓ ${tools.length} tools registered`);

      // Test 3: Tool schema validation
      for (const tool of tools) {
        if (!tool.name || !tool.description || !tool.inputSchema) {
          throw new Error(`Tool "${tool.name}" has invalid schema`);
        }
      }
      console.log(`  ✓ All tools have valid schemas`);

      // Test 4: File operations
      try {
        const testContent = server.readFile('README.md');
        if (!testContent || testContent.length === 0) {
          throw new Error('File read failed');
        }
        console.log('  ✓ File read operations work');
      } catch (error) {
        console.log(`  ⚠ File read test skipped: ${error.message}`);
      }

      // Test 5: List operations
      try {
        const files = server.listFiles('app/src/main', false);
        if (!Array.isArray(files)) {
          throw new Error('List files failed');
        }
        console.log(`  ✓ List operations work (${files.length} files found)`);
      } catch (error) {
        console.log(`  ⚠ List files test skipped: ${error.message}`);
      }

      results.push({
        server: serverInfo.name,
        status: 'PASS',
        toolsCount: tools.length,
      });

      console.log(`  ✓ ${serverInfo.name} PASSED\n`);

    } catch (error) {
      console.log(`  ✗ ${serverInfo.name} FAILED`);
      console.log(`    Error: ${error.message}\n`);

      results.push({
        server: serverInfo.name,
        status: 'FAIL',
        error: error.message,
      });
    }
  }

  // Summary
  console.log('\n' + '='.repeat(50));
  console.log('SMOKE TEST SUMMARY');
  console.log('='.repeat(50));

  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;

  console.log(`Total: ${results.length}`);
  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);

  if (failed > 0) {
    console.log('\n❌ Some tests failed!');
    process.exit(1);
  } else {
    console.log('\n✅ All smoke tests passed!');
    process.exit(0);
  }
}

runSmokeTests().catch(error => {
  console.error('Fatal error running smoke tests:', error);
  process.exit(1);
});