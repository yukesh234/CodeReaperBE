package com.codereaper.Service.indexing;


import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class CodeFileFilter {
    private static final Set<String> SKIP_DIR_PARTS = Set.of(
            "node_modules", ".git", "dist", "target", ".next", "vendor",
            ".idea", ".vscode", "coverage", "__pycache__", "out", "build",
            ".gradle", ".venv", "venv"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "java", "kt", "kts", "scala",
            "c", "h", "cpp", "hpp", "cs",
            "ts", "tsx", "js", "jsx", "mjs", "cjs",
            "go", "py", "rs", "rb", "php",
            "vue", "yaml", "yml", "swift", "json","md",
            "xml", "scss","svelte","dockerfile",
            "sh", "mdx", "gradle","zsh","makefile","bash",
            "txt", "m", "mm","html", "css","properties","toml",
            "sass"
    );

    private static final Set<String> SKIP_FILENAMES = Set.of(
            "package-lock.json","yarn.lock",
            "pnpm-lock.yaml","composer.lock","cargo.lock","poetry.lock"
    );

//    private static final long MAX_FILE_BYTES = 200_000;
    public boolean isEligible(String path, long sizeBytes, long maxFileBytes){
        if (path == null) {
            return false;
        }
        String normalized = path.replace('\\','/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        for(String parts : lower.split("/")){
            if(SKIP_DIR_PARTS.contains(parts)){
                return false;
            }
        }

        String filename = lower.substring(lower.lastIndexOf('/')+1);
        if(SKIP_FILENAMES.contains(filename)){
            return false;
        }
        if(filename.startsWith(".")){
            return false;
        }
        if(sizeBytes > maxFileBytes){
            return false;
        }
        if("dockerfile".equals(filename) || "makefile".equals(filename)){
            return true;
        }
        int dot = filename.lastIndexOf('.');
        if(dot<0){
            return false;
        }
        String extension = filename.substring(dot+1);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    public String detectLanguage(String path){
        String lower = path.toLowerCase(Locale.ROOT);
        String fileName = lower.substring(lower.lastIndexOf('/')+1);

        if("dockerfile".equals(fileName)){
            return "dockerfile";
        }
        if("makefile".equals(fileName)){
            return "makefile";
        }

        int dot = fileName.lastIndexOf('.');
        if(dot<0){
            return "text";
        }
        return fileName.substring(dot+1);
    }
}
