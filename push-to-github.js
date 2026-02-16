const { execSync } = require('child_process');
const path = require('path');

process.chdir(path.join(__dirname));

try {
    // Kill stuck processes
    try { execSync('pkill -9 git', { stdio: 'ignore' }); } catch(e) {}
    try { execSync('pkill -9 vim', { stdio: 'ignore' }); } catch(e) {}
    
    // Clean rebase
    try { execSync('rm -rf .git/rebase-merge .git/rebase-apply', { stdio: 'ignore' }); } catch(e) {}
    
    // Check current commit
    const current = execSync('git log -1 --format="%H %an <%ae>"', { encoding: 'utf8' });
    console.log('Current:', current.trim());
    
    // Fix author if needed
    if (!current.includes('muzimu217')) {
        console.log('Fixing author...');
        execSync('git commit --amend --no-edit --reset-author', {
            env: {
                ...process.env,
                GIT_AUTHOR_NAME: 'muzimu217',
                GIT_AUTHOR_EMAIL: 'muzimu217@users.noreply.github.com',
                GIT_COMMITTER_NAME: 'muzimu217',
                GIT_COMMITTER_EMAIL: 'muzimu217@users.noreply.github.com'
            }
        });
        console.log('Author fixed');
    }
    
    // Push
    console.log('Pushing...');
    const output = execSync('git push origin main --force-with-lease', { encoding: 'utf8' });
    console.log(output);
    console.log('✓ Success!');
    
} catch(error) {
    console.error('Error:', error.message);
    if (error.stdout) console.log(error.stdout.toString());
    if (error.stderr) console.error(error.stderr.toString());
    process.exit(1);
}
