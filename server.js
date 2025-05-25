const express = require('express');
const { exec, spawn } = require('child_process');
const fs = require('fs').promises;
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;
const TEMP_DIR = path.join(__dirname, 'temp');

// Enhanced Compiler Configuration
const COMPILER_CONFIG = {
    // Primary method: Use JAR file directly with Java
    USE_JAVA_DIRECT: true,
    JAR_PATH: './MyCompiler.jar',
    
    // Alternative method: Use EXE wrapper
    EXE_PATH: './MyCompiler.exe',
    
    // Java configuration
    JAVA_OPTIONS: [
        '-Xms256m',      // Initial heap size
        '-Xmx1024m',     // Maximum heap size
        '-XX:+UseG1GC'   // Use G1 garbage collector for better performance
    ],
    
    // Execution settings
    TIMEOUT: 45000,      // 45 seconds timeout
    MAX_BUFFER: 1024 * 1024 * 20, // 20MB buffer
};

// Middleware
app.use(cors({
    origin: process.env.NODE_ENV === 'production' ? false : true,
    credentials: true
}));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(express.static('public'));

// Security headers (basic)
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Initialize application
async function initializeApp() {
    try {
        await ensureTempDir();
        await verifyCompilerSetup();
        console.log('✅ Application initialized successfully');
    } catch (error) {
        console.error('❌ Initialization failed:', error.message);
        process.exit(1);
    }
}

// Ensure temp directory exists
async function ensureTempDir() {
    try {
        await fs.access(TEMP_DIR);
        console.log('📁 Temp directory exists:', TEMP_DIR);
    } catch {
        await fs.mkdir(TEMP_DIR, { recursive: true });
        console.log('📁 Created temp directory:', TEMP_DIR);
    }
}

// Verify compiler setup
async function verifyCompilerSetup() {
    const checks = [];
    
    // Check Java installation
    checks.push(checkJavaInstallation());
    
    // Check compiler files
    if (COMPILER_CONFIG.USE_JAVA_DIRECT) {
        checks.push(checkJarFile());
    } else {
        checks.push(checkExeFile());
    }
    
    const results = await Promise.allSettled(checks);
    const failures = results.filter(r => r.status === 'rejected');
    
    if (failures.length > 0) {
        throw new Error(`Setup verification failed: ${failures.map(f => f.reason).join(', ')}`);
    }
}

// Check Java installation
async function checkJavaInstallation() {
    return new Promise((resolve, reject) => {
        exec('java -version', (error, stdout, stderr) => {
            if (error) {
                reject('Java not found. Please install Java JDK/JRE and add to PATH');
            } else {
                const version = stderr || stdout;
                console.log('☕ Java version detected:', version.split('\n')[0]);
                resolve();
            }
        });
    });
}

// Check JAR file
async function checkJarFile() {
    try {
        await fs.access(COMPILER_CONFIG.JAR_PATH);
        console.log('📦 JAR file found:', COMPILER_CONFIG.JAR_PATH);
    } catch {
        throw new Error(`JAR file not found: ${COMPILER_CONFIG.JAR_PATH}`);
    }
}

// Check EXE file
async function checkExeFile() {
    try {
        await fs.access(COMPILER_CONFIG.EXE_PATH);
        console.log('⚙️ EXE file found:', COMPILER_CONFIG.EXE_PATH);
    } catch {
        throw new Error(`EXE file not found: ${COMPILER_CONFIG.EXE_PATH}`);
    }
}

// Enhanced output parsing specifically for your compiler format
function parseCompilerOutput(output) {
    if (!output || !output.trim()) {
        return { 'No Output': 'Compiler produced no output' };
    }

    const phases = {};
    const lines = output.split('\n');
    let currentPhase = '';
    let currentContent = [];
    
    // Define phase patterns specific to your compiler
    const phasePatterns = [
        { pattern: /^Phase \d+: (.+)/i, name: match => match[1] },
        { pattern: /^=+ (.+) =+/i, name: match => match[1] },
        { pattern: /^=+ (.+) Started =+/i, name: match => match[1] },
        { pattern: /^=+ (.+) Completed =+/i, name: match => match[1] + ' (Completed)' },
        { pattern: /^=+ PROGRAM SUMMARY =+/i, name: () => 'Program Summary' },
        { pattern: /^=+ PARSER INITIALIZED =+/i, name: () => 'Parser Initialization' },
        { pattern: /^Instructions before (.+):/i, name: match => 'Before ' + match[1] },
        { pattern: /^Instructions after (.+):/i, name: match => 'After ' + match[1] },
        { pattern: /^Optimized IR:/i, name: () => 'Optimized Intermediate Representation' },
        { pattern: /^Tokens:/i, name: () => 'Token List' },
        { pattern: /^Total tokens:/i, name: () => 'Token Summary' }
    ];
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmedLine = line.trim();
        
        // Check for phase start patterns
        let phaseFound = false;
        for (const { pattern, name } of phasePatterns) {
            const match = trimmedLine.match(pattern);
            if (match) {
                // Save previous phase if exists
                if (currentPhase && currentContent.length > 0) {
                    phases[currentPhase] = currentContent.join('\n').trim();
                }
                
                // Start new phase
                currentPhase = name(match);
                currentContent = [line]; // Include the header line
                phaseFound = true;
                break;
            }
        }
        
        // Special handling for specific sections
        if (!phaseFound) {
            // Check for compilation completion
            if (trimmedLine.includes('Compilation completed successfully')) {
                if (currentPhase && currentContent.length > 0) {
                    phases[currentPhase] = currentContent.join('\n').trim();
                }
                currentPhase = 'Compilation Result';
                currentContent = [line];
            }
            // Check for function definitions in assembly
            else if (trimmedLine.startsWith('Function:') && currentPhase.includes('Generation')) {
                currentContent.push(line);
            }
            // Add to current phase
            else {
                currentContent.push(line);
            }
        }
    }
    
    // Save the last phase
    if (currentPhase && currentContent.length > 0) {
        phases[currentPhase] = currentContent.join('\n').trim();
    }
    
    // If no phases were detected, try a fallback approach
    if (Object.keys(phases).length === 0) {
        // Split by double newlines or major separators
        const sections = output.split(/\n\s*\n/);
        sections.forEach((section, index) => {
            if (section.trim()) {
                const firstLine = section.trim().split('\n')[0];
                let sectionName = `Section ${index + 1}`;
                
                // Try to extract meaningful names from first line
                if (firstLine.includes('Phase')) {
                    sectionName = firstLine;
                } else if (firstLine.includes('===')) {
                    sectionName = firstLine.replace(/=/g, '').trim();
                } else if (firstLine.length < 50) {
                    sectionName = firstLine;
                }
                
                phases[sectionName] = section.trim();
            }
        });
    }
    
    // Ensure we have some output
    if (Object.keys(phases).length === 0) {
        phases['Compiler Output'] = output.trim();
    }
    
    // Clean up empty phases
    Object.keys(phases).forEach(key => {
        if (!phases[key] || !phases[key].trim()) {
            delete phases[key];
        }
    });
    
    return phases;
}

// Generate unique filename with better collision avoidance
function generateUniqueFilename() {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 12);
    const pid = process.pid;
    return `JavaFile_${timestamp}_${pid}_${random}`;
}

// Build compiler command
function buildCompilerCommand(javaFilePath) {
    if (COMPILER_CONFIG.USE_JAVA_DIRECT) {
        const javaOptions = COMPILER_CONFIG.JAVA_OPTIONS.join(' ');
        return `java ${javaOptions} -jar "${COMPILER_CONFIG.JAR_PATH}" "${javaFilePath}"`;
    } else {
        return `"${COMPILER_CONFIG.EXE_PATH}" "${javaFilePath}"`;
    }
}

// Enhanced compilation endpoint
app.post('/compile', async (req, res) => {
    const { code, options = {} } = req.body;
    
    // Input validation
    if (!code || !code.trim()) {
        return res.status(400).json({ 
            success: false,
            error: 'No Java code provided',
            timestamp: new Date().toISOString()
        });
    }
    
    // Basic Java syntax check
    if (!code.includes('class') && !code.includes('interface')) {
        return res.status(400).json({
            success: false,
            error: 'Code must contain at least one class or interface declaration',
            timestamp: new Date().toISOString()
        });
    }
    
    const filename = generateUniqueFilename();
    const javaFilePath = path.join(TEMP_DIR, `${filename}.java`);
    const startTime = Date.now();
    
    try {
        // Write Java code to file
        await fs.writeFile(javaFilePath, code, 'utf8');
        console.log(`📝 Created temp file: ${javaFilePath}`);
        
        // Build command
        const command = buildCompilerCommand(javaFilePath);
        console.log(`🔧 Executing: ${command}`);
        
        // Execute compiler
        const result = await executeCompiler(command, javaFilePath);
        const executionTime = Date.now() - startTime;
        
        // Clean up
        await cleanupFile(javaFilePath);
        
        // Parse and return results
        const phases = parseCompilerOutput(result.output);
        
        console.log('📊 Parsed phases:', Object.keys(phases));
        
        res.json({
            success: result.success,
            phases: phases,
            rawOutput: result.output,
            executionTime: `${executionTime} ms`,
            timestamp: new Date().toISOString(),
            compiler: COMPILER_CONFIG.USE_JAVA_DIRECT ? 'Java JAR' : 'EXE',
            phaseCount: Object.keys(phases).length,
            ...(result.error && { error: result.error })
        });
        
    } catch (error) {
        console.error('❌ Compilation error:', error);
        
        // Cleanup on error
        await cleanupFile(javaFilePath);
        
        res.status(500).json({
            success: false,
            error: `Compilation failed: ${error.message}`,
            timestamp: new Date().toISOString(),
            executionTime: `${Date.now() - startTime} ms`
        });
    }
});

// Execute compiler with enhanced error handling
function executeCompiler(command, javaFilePath) {
    return new Promise((resolve) => {
        const env = {
            ...process.env,
            JAVA_HOME: process.env.JAVA_HOME || '',
            PATH: process.env.PATH
        };
        
        exec(command, {
            cwd: __dirname,
            timeout: COMPILER_CONFIG.TIMEOUT,
            maxBuffer: COMPILER_CONFIG.MAX_BUFFER,
            env: env,
            windowsHide: true
        }, (error, stdout, stderr) => {
            const output = stdout + (stderr ? '\n' + stderr : '');
            
            if (error) {
                if (error.code === 'ETIMEDOUT') {
                    resolve({
                        success: false,
                        output: 'Compilation timeout - process took too long',
                        error: 'Timeout'
                    });
                } else {
                    resolve({
                        success: false,
                        output: output || error.message,
                        error: error.message
                    });
                }
            } else {
                resolve({
                    success: true,
                    output: output
                });
            }
        });
    });
}

// Clean up temporary files
async function cleanupFile(filePath) {
    try {
        await fs.unlink(filePath);
        console.log(`🗑️ Cleaned up: ${filePath}`);
    } catch (error) {
        console.warn(`⚠️ Cleanup warning: ${error.message}`);
    }
}

// System status endpoint
app.get('/status', async (req, res) => {
    const status = {
        server: 'OK',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
        memory: process.memoryUsage(),
        compiler: {
            type: COMPILER_CONFIG.USE_JAVA_DIRECT ? 'Java JAR' : 'EXE',
            path: COMPILER_CONFIG.USE_JAVA_DIRECT ? COMPILER_CONFIG.JAR_PATH : COMPILER_CONFIG.EXE_PATH
        },
        java: null,
        tempDir: TEMP_DIR
    };
    
    // Check Java
    try {
        await checkJavaInstallation();
        status.java = 'Available';
    } catch (error) {
        status.java = 'Not Available: ' + error;
    }
    
    // Check temp directory
    try {
        const files = await fs.readdir(TEMP_DIR);
        status.tempFiles = files.length;
    } catch {
        status.tempFiles = 'Unknown';
    }
    
    res.json(status);
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ 
        status: 'OK', 
        timestamp: new Date().toISOString(),
        compiler: COMPILER_CONFIG.USE_JAVA_DIRECT ? COMPILER_CONFIG.JAR_PATH : COMPILER_CONFIG.EXE_PATH
    });
});

// Test endpoint for debugging output parsing
app.post('/test-parse', (req, res) => {
    const { output } = req.body;
    
    if (!output) {
        return res.status(400).json({ error: 'No output provided for testing' });
    }
    
    const phases = parseCompilerOutput(output);
    
    res.json({
        success: true,
        phases: phases,
        phaseCount: Object.keys(phases).length,
        phaseNames: Object.keys(phases)
    });
});

// Serve frontend
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Error handling middleware
app.use((error, req, res, next) => {
    console.error('Server error:', error);
    res.status(500).json({
        success: false,
        error: 'Internal server error',
        timestamp: new Date().toISOString()
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        error: 'Endpoint not found',
        path: req.path
    });
});

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n🛑 Shutting down server...');
    
    try {
        // Clean up all temporary files
        const files = await fs.readdir(TEMP_DIR);
        const cleanupPromises = files
            .filter(file => file.startsWith('JavaFile_'))
            .map(file => fs.unlink(path.join(TEMP_DIR, file)).catch(() => {}));
        
        await Promise.all(cleanupPromises);
        console.log(`🧹 Cleaned up ${cleanupPromises.length} temporary files`);
        
        console.log('✅ Graceful shutdown completed');
    } catch (error) {
        console.log('⚠️ Error during cleanup:', error.message);
    }
    
    process.exit(0);
});

// Start server
async function startServer() {
    try {
        await initializeApp();
        
        app.listen(PORT, () => {
            console.log('\n🚀 Enhanced Java Compiler Server Started!');
            console.log(`📡 Server: http://localhost:${PORT}`);
            console.log(`📁 Temp directory: ${TEMP_DIR}`);
            console.log(`🔧 Compiler: ${COMPILER_CONFIG.USE_JAVA_DIRECT ? 'Java JAR' : 'EXE'}`);
            console.log(`📦 File: ${COMPILER_CONFIG.USE_JAVA_DIRECT ? COMPILER_CONFIG.JAR_PATH : COMPILER_CONFIG.EXE_PATH}`);
            console.log(`⏱️ Timeout: ${COMPILER_CONFIG.TIMEOUT / 1000}s`);
            
            console.log('\n🔗 Available Endpoints:');
            console.log('  GET  /         - Frontend interface');
            console.log('  POST /compile  - Compile Java code');
            console.log('  GET  /status   - System status');
            console.log('  GET  /health   - Health check');
            console.log('  POST /test-parse - Test output parsing');
        });
        
    } catch (error) {
        console.error('❌ Failed to start server:', error);
        process.exit(1);
    }
}
startServer();