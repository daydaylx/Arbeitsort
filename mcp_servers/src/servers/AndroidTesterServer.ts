import { McpServerBase, McpServerContext, Tool } from '../base/McpServerBase.js';
import * as fs from 'fs';
import * as path from 'path';

interface TestFile {
  path: string;
  content: string;
  type: 'unit' | 'instrumented';
}

interface TestGenerationResult {
  files: TestFile[];
  summary: {
    totalFiles: number;
    unitTests: number;
    instrumentedTests: number;
  };
}

interface TestCoverageReport {
  file: string;
  coverage: {
    functions: number;
    lines: number;
    branches: number;
  };
  suggestions: string[];
}

export class AndroidTesterServer extends McpServerBase {
  constructor(context: McpServerContext) {
    super('android-tester', '1.0.0', context);
  }

  protected registerTools(): void {
    this.tools = [
      {
        name: 'generate_unit_tests',
        description: 'Generate unit tests for a Kotlin file based on existing code',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to Kotlin file to generate tests for',
            },
            testFramework: {
              type: 'string',
              enum: ['junit', 'mockk'],
              description: 'Test framework to use',
              default: 'mockk',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'generate_instrumented_tests',
        description: 'Generate instrumented tests for Android components',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to Android component file',
            },
            componentType: {
              type: 'string',
              enum: ['activity', 'fragment', 'service', 'receiver', 'viewmodel'],
              description: 'Type of Android component',
            },
          },
          required: ['filePath', 'componentType'],
        },
      },
      {
        name: 'analyze_test_coverage',
        description: 'Analyze test coverage for a file and suggest improvements',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to source file',
            },
            testFilePath: {
              type: 'string',
              description: 'Path to corresponding test file',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'generate_usecase_tests',
        description: 'Generate tests for UseCase classes following Clean Architecture',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to UseCase file',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'generate_viewmodel_tests',
        description: 'Generate tests for ViewModel classes',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: 'Path to ViewModel file',
            },
          },
          required: ['filePath'],
        },
      },
    ];
  }

  protected async handleToolCall(toolName: string, args: any): Promise<any> {
    switch (toolName) {
      case 'generate_unit_tests':
        return this.generateUnitTests(args.filePath, args.testFramework);
      case 'generate_instrumented_tests':
        return this.generateInstrumentedTests(args.filePath, args.componentType);
      case 'analyze_test_coverage':
        return this.analyzeTestCoverage(args.filePath, args.testFilePath);
      case 'generate_usecase_tests':
        return this.generateUseCaseTests(args.filePath);
      case 'generate_viewmodel_tests':
        return this.generateViewModelTests(args.filePath);
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  private async generateUnitTests(filePath: string, framework: string = 'mockk'): Promise<TestGenerationResult> {
    this.context.logger.info(`Generating unit tests for: ${filePath}`);

    const content = this.readFile(filePath);
    const className = this.extractClassName(content);
    const testContent = this.generateTestClass(className, content, framework);

    const testFilePath = filePath.replace('.kt', 'Test.kt').replace('/main/', '/test/');
    const testDir = path.dirname(testFilePath);

    return {
      files: [
        {
          path: testFilePath,
          content: testContent,
          type: 'unit',
        },
      ],
      summary: {
        totalFiles: 1,
        unitTests: 1,
        instrumentedTests: 0,
      },
    };
  }

  private async generateInstrumentedTests(filePath: string, componentType: string): Promise<TestGenerationResult> {
    this.context.logger.info(`Generating instrumented tests for: ${filePath} (${componentType})`);

    const content = this.readFile(filePath);
    const className = this.extractClassName(content);
    const testContent = this.generateInstrumentedTestClass(className, content, componentType);

    const testFilePath = filePath.replace('.kt', 'Test.kt').replace('/main/', '/androidTest/');
    const testDir = path.dirname(testFilePath);

    return {
      files: [
        {
          path: testFilePath,
          content: testContent,
          type: 'instrumented',
        },
      ],
      summary: {
        totalFiles: 1,
        unitTests: 0,
        instrumentedTests: 1,
      },
    };
  }

  private async analyzeTestCoverage(filePath: string, testFilePath?: string): Promise<TestCoverageReport> {
    this.context.logger.info(`Analyzing test coverage for: ${filePath}`);

    const content = this.readFile(filePath);
    const testContent = testFilePath ? this.readFile(testFilePath) : '';

    const functions = this.extractFunctions(content);
    const testedFunctions = testContent ? this.extractTestedFunctions(testContent) : [];

    const coverage = {
      functions: Math.round((testedFunctions.length / functions.length) * 100),
      lines: this.estimateLineCoverage(content, testContent),
      branches: 0, // Would need actual coverage tool
    };

    const suggestions = this.generateCoverageSuggestions(functions, testedFunctions, coverage);

    return {
      file: filePath,
      coverage,
      suggestions,
    };
  }

  private async generateUseCaseTests(filePath: string): Promise<TestGenerationResult> {
    this.context.logger.info(`Generating UseCase tests for: ${filePath}`);

    const content = this.readFile(filePath);
    const className = this.extractClassName(content);
    const testContent = this.generateUseCaseTestClass(className, content);

    const testFilePath = filePath.replace('.kt', 'Test.kt').replace('/main/', '/test/');
    const testDir = path.dirname(testFilePath);

    return {
      files: [
        {
          path: testFilePath,
          content: testContent,
          type: 'unit',
        },
      ],
      summary: {
        totalFiles: 1,
        unitTests: 1,
        instrumentedTests: 0,
      },
    };
  }

  private async generateViewModelTests(filePath: string): Promise<TestGenerationResult> {
    this.context.logger.info(`Generating ViewModel tests for: ${filePath}`);

    const content = this.readFile(filePath);
    const className = this.extractClassName(content);
    const testContent = this.generateViewModelTestClass(className, content);

    const testFilePath = filePath.replace('.kt', 'Test.kt').replace('/main/', '/test/');
    const testDir = path.dirname(testFilePath);

    return {
      files: [
        {
          path: testFilePath,
          content: testContent,
          type: 'unit',
        },
      ],
      summary: {
        totalFiles: 1,
        unitTests: 1,
        instrumentedTests: 0,
      },
    };
  }

  private extractClassName(content: string): string {
    const match = content.match(/class\s+(\w+)/);
    return match ? match[1] : 'UnknownClass';
  }

  private extractFunctions(content: string): string[] {
    const functionMatches = content.matchAll(/fun\s+(\w+)\s*\(/g);
    return Array.from(functionMatches).map(m => m[1]);
  }

  private extractTestedFunctions(testContent: string): string[] {
    const testMatches = testContent.matchAll(/@Test\s+fun\s+`test(\w+)`/g);
    return Array.from(testMatches).map(m => m[1]);
  }

  private estimateLineCoverage(sourceContent: string, testContent: string): number {
    if (!testContent) return 0;
    const sourceLines = sourceContent.split('\n').filter(l => l.trim() && !l.trim().startsWith('//'));
    const testLines = testContent.split('\n').filter(l => l.trim() && !l.trim().startsWith('//'));
    return Math.min(Math.round((testLines.length / sourceLines.length) * 100), 100);
  }

  private generateCoverageSuggestions(functions: string[], testedFunctions: string[], coverage: any): string[] {
    const suggestions: string[] = [];

    const untestedFunctions = functions.filter(f => !testedFunctions.includes(f));
    if (untestedFunctions.length > 0) {
      suggestions.push(`Add tests for untested functions: ${untestedFunctions.join(', ')}`);
    }

    if (coverage.functions < 50) {
      suggestions.push('Function coverage is below 50%. Consider adding more test cases.');
    }

    if (coverage.lines < 60) {
      suggestions.push('Line coverage is below 60%. Add edge case tests.');
    }

    if (suggestions.length === 0) {
      suggestions.push('Good coverage! Consider adding integration tests.');
    }

    return suggestions;
  }

  private generateTestClass(className: string, content: string, framework: string): string {
    const functions = this.extractFunctions(content);
    const imports = this.generateImports(framework);
    const testMethods = functions.map(fn => this.generateTestMethod(fn, framework)).join('\n\n');

    return `${imports}

class ${className}Test {

    ${testMethods}
}
`;
  }

  private generateInstrumentedTestClass(className: string, content: string, componentType: string): string {
    const imports = `import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ${className}Test {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testComponentExists() {
        // TODO: Implement test for ${componentType}
        assertNotNull("${className} should be instantiated")
    }
}
`;
    return imports;
  }

  private generateUseCaseTestClass(className: string, content: string): string {
    const imports = `import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ${className}Test {

    private lateinit var useCase: ${className}
    private val mockRepository = mockk<Repository>()

    @Before
    fun setup() {
        useCase = ${className}(mockRepository)
    }

    @Test
    fun \`execute returns success\`() = runTest {
        // Given
        val expectedResult = Result.success(Unit)

        // When
        every { mockRepository.someMethod() } returns expectedResult

        // Then
        val result = useCase.execute()
        assertEquals(expectedResult, result)
        verify { mockRepository.someMethod() }
    }
}
`;
    return imports;
  }

  private generateViewModelTestClass(className: string, content: string): string {
    const imports = `import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ${className}Test {

    private lateinit var viewModel: ${className}
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // TODO: Initialize viewModel with mocked dependencies
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun \`initial state is correct\`() {
        // Then
        // TODO: Assert initial state
    }
}
`;
    return imports;
  }

  private generateTestMethod(functionName: string, framework: string): string {
    return `    @Test
    fun \`test${functionName}\`() {
        // TODO: Implement test for ${functionName}
        // Given
        // When
        // Then
    }`;
  }

  private generateImports(framework: string): string {
    if (framework === 'mockk') {
      return `import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*`;
    }
    return `import org.junit.Before
import org.junit.Test
import org.junit.Assert.*`;
  }
}
