MPP Viewer - portable build
===========================
1. Create a free account on github.com and make a new repository.
2. Upload ALL of these files, keeping the folders (pom.xml, src/, .github/).
   Tip: drag the whole unzipped folder into the "Add file > Upload files" page.
3. Open the "Actions" tab. The "Build portable MPP Viewer" job starts by itself
   (or press "Run workflow"). It takes about 3-5 minutes.
4. When it finishes, open the run and download the artifact:
     MPPViewer-windows-latest  -> Windows
     MPPViewer-macos-latest    -> Mac
     MPPViewer-ubuntu-latest   -> Linux
5. Unzip it. Inside is a MPPViewer folder. On Windows run MPPViewer\MPPViewer.exe.
   Java is bundled; nothing to install. Copy the folder anywhere / to a USB stick.

Click the program, choose the .mpp file, and the plan opens in your browser
(task table + Gantt chart, search, critical-path filter). Nothing is uploaded.
