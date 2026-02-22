#!/usr/bin/env python3
"""
Codebase Structure Analyzer — produces a JSON summary of a project.

Usage:
    python analyze-structure.py <project_dir>

Output (JSON):
    {
        "project_type": "gradle|maven|npm|pip|go|dotnet|unknown",
        "languages": ["kotlin", "java", ...],
        "framework": "spring-boot|django|fastapi|express|...",
        "stats": { "total_files": N, "total_loc": N, "source_files": N, "test_files": N },
        "structure": { "top_dirs": [...], "max_depth": N },
        "entities": [...],
        "controllers": [...],
        "services": [...],
        "configurations": [...],
        "dependencies": [...]
    }
"""

import json
import os
import re
import sys
from pathlib import Path
from collections import defaultdict

IGNORE_DIRS = {
    '.git', 'node_modules', '__pycache__', '.gradle', 'build', 'dist',
    'target', '.idea', '.vscode', 'venv', '.venv', '.DS_Store', 'bin', 'obj'
}

LANGUAGE_EXTENSIONS = {
    '.kt': 'kotlin', '.java': 'java', '.py': 'python', '.go': 'go',
    '.ts': 'typescript', '.tsx': 'typescript', '.js': 'javascript',
    '.jsx': 'javascript', '.cs': 'csharp', '.rs': 'rust',
    '.rb': 'ruby', '.swift': 'swift', '.scala': 'scala',
    '.sql': 'sql', '.sh': 'shell', '.yml': 'yaml', '.yaml': 'yaml',
    '.json': 'json', '.md': 'markdown', '.html': 'html', '.css': 'css',
    '.scss': 'scss', '.xml': 'xml', '.gradle': 'gradle',
}

SOURCE_EXTENSIONS = {'.kt', '.java', '.py', '.go', '.ts', '.tsx', '.js', '.jsx', '.cs', '.rs', '.rb', '.scala'}


def detect_project_type(root: Path) -> str:
    if (root / 'build.gradle.kts').exists() or (root / 'build.gradle').exists():
        return 'gradle'
    if (root / 'pom.xml').exists():
        return 'maven'
    if (root / 'package.json').exists():
        return 'npm'
    if (root / 'go.mod').exists():
        return 'go'
    if (root / 'pyproject.toml').exists() or (root / 'setup.py').exists() or (root / 'requirements.txt').exists():
        return 'pip'
    if any(root.glob('*.sln')) or any(root.glob('*.csproj')):
        return 'dotnet'
    return 'unknown'


def detect_framework(root: Path, project_type: str, languages: list) -> str:
    if project_type in ('gradle', 'maven'):
        for gf in root.rglob('build.gradle*'):
            try:
                content = gf.read_text(errors='ignore')
                if 'spring-boot' in content or 'org.springframework.boot' in content:
                    return 'spring-boot'
            except:
                pass
        for pom in root.rglob('pom.xml'):
            try:
                content = pom.read_text(errors='ignore')
                if 'spring-boot' in content:
                    return 'spring-boot'
            except:
                pass
    if project_type == 'npm':
        try:
            pkg = json.loads((root / 'package.json').read_text())
            deps = {**pkg.get('dependencies', {}), **pkg.get('devDependencies', {})}
            if 'next' in deps:
                return 'nextjs'
            if 'express' in deps:
                return 'express'
            if 'react' in deps:
                return 'react'
            if 'vue' in deps:
                return 'vue'
            if 'angular' in deps or '@angular/core' in deps:
                return 'angular'
        except:
            pass
    if 'python' in languages:
        for f in root.rglob('*.py'):
            try:
                content = f.read_text(errors='ignore')[:2000]
                if 'from fastapi' in content or 'import fastapi' in content:
                    return 'fastapi'
                if 'from django' in content or 'import django' in content:
                    return 'django'
                if 'from flask' in content or 'import flask' in content:
                    return 'flask'
            except:
                pass
    return 'unknown'


def scan_files(root: Path):
    stats = {'total_files': 0, 'total_loc': 0, 'source_files': 0, 'test_files': 0}
    lang_counter = defaultdict(int)
    top_dirs = set()
    max_depth = 0

    for dirpath, dirnames, filenames in os.walk(root):
        # Filter ignored directories
        dirnames[:] = [d for d in dirnames if d not in IGNORE_DIRS]

        rel = os.path.relpath(dirpath, root)
        depth = 0 if rel == '.' else rel.count(os.sep) + 1
        max_depth = max(max_depth, depth)

        if depth == 1:
            top_dirs.add(rel.split(os.sep)[0])

        for fname in filenames:
            fpath = Path(dirpath) / fname
            ext = fpath.suffix.lower()

            stats['total_files'] += 1

            if ext in SOURCE_EXTENSIONS:
                stats['source_files'] += 1
                # Count lines
                try:
                    loc = sum(1 for _ in fpath.open(errors='ignore'))
                    stats['total_loc'] += loc
                except:
                    pass

            if ext in LANGUAGE_EXTENSIONS:
                lang_counter[LANGUAGE_EXTENSIONS[ext]] += 1

            # Detect test files
            name_lower = fname.lower()
            if ('test' in name_lower or 'spec' in name_lower) and ext in SOURCE_EXTENSIONS:
                stats['test_files'] += 1

    languages = [lang for lang, count in sorted(lang_counter.items(), key=lambda x: -x[1])]
    return stats, languages, sorted(top_dirs), max_depth


def scan_annotations(root: Path, pattern: str, label: str, extensions=('.kt', '.java')):
    """Scan for annotated classes (e.g., @Entity, @RestController)."""
    results = []
    regex = re.compile(pattern)

    for ext in extensions:
        for fpath in root.rglob(f'*{ext}'):
            try:
                content = fpath.read_text(errors='ignore')
                for match in regex.finditer(content):
                    # Try to extract class name from next line
                    pos = match.end()
                    remaining = content[pos:pos+500]
                    class_match = re.search(r'(?:class|interface|object)\s+(\w+)', remaining)
                    if class_match:
                        results.append({
                            'name': class_match.group(1),
                            'file': str(fpath.relative_to(root)),
                            'type': label
                        })
            except:
                pass

    return results


def scan_dependencies(root: Path, project_type: str):
    """Extract key dependencies."""
    deps = []

    if project_type in ('gradle', 'maven'):
        for gf in root.rglob('build.gradle*'):
            try:
                content = gf.read_text(errors='ignore')
                for m in re.finditer(r'implementation\s*[("\']+([^"\')\s]+)', content):
                    dep = m.group(1)
                    if ':' in dep:
                        deps.append(dep)
            except:
                pass

    if project_type == 'npm':
        try:
            pkg = json.loads((root / 'package.json').read_text())
            for dep_name in pkg.get('dependencies', {}):
                deps.append(dep_name)
        except:
            pass

    return deps[:30]  # Cap at 30


def scan_config_files(root: Path):
    """Find configuration files."""
    config_patterns = [
        'application.yml', 'application.yaml', 'application.properties',
        '.env', '.env.example', 'docker-compose*.yml', 'Dockerfile',
        'tsconfig.json', 'next.config.*', 'webpack.config.*',
        'settings.gradle*', 'gradle.properties',
    ]
    configs = []
    for pattern in config_patterns:
        for f in root.glob(pattern):
            configs.append(str(f.relative_to(root)))
        for f in root.glob(f'**/{pattern}'):
            rel = str(f.relative_to(root))
            if rel not in configs and not any(ig in rel for ig in IGNORE_DIRS):
                configs.append(rel)
    return sorted(set(configs))[:20]


def main():
    if len(sys.argv) < 2:
        print(json.dumps({'error': 'Usage: analyze-structure.py <project_dir>'}))
        sys.exit(1)

    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        print(json.dumps({'error': f'Not a directory: {root}'}))
        sys.exit(1)

    project_type = detect_project_type(root)
    stats, languages, top_dirs, max_depth = scan_files(root)
    framework = detect_framework(root, project_type, languages)

    entities = scan_annotations(root, r'@Entity\b', 'entity')
    controllers = scan_annotations(root, r'@(?:Rest)?Controller\b', 'controller')
    services = scan_annotations(root, r'@Service\b', 'service')

    # For Python/TS, scan for class definitions in common patterns
    if 'python' in languages:
        entities += scan_annotations(root, r'class\s+\w+\(.*(?:Model|Base)\)', 'entity', ('.py',))
        controllers += scan_annotations(root, r'@(?:app|router)\.\w+\(', 'controller', ('.py',))

    deps = scan_dependencies(root, project_type)
    configs = scan_config_files(root)

    result = {
        'project_type': project_type,
        'languages': languages,
        'framework': framework,
        'stats': stats,
        'structure': {
            'top_dirs': top_dirs,
            'max_depth': max_depth,
        },
        'entities': entities,
        'controllers': controllers,
        'services': services,
        'configurations': configs,
        'dependencies': deps,
    }

    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
