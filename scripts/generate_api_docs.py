#!/usr/bin/env python3
"""
API Documentation Generator for FISCO BCOS Supply Chain Finance Platform
Reads Swagger annotations from controller files and generates Markdown documentation.
"""

import os
import re
from pathlib import Path
from typing import List, Optional, Dict
from dataclasses import dataclass, field

@dataclass
class ApiResponse:
    code: str
    message: str

@dataclass
class ApiEndpoint:
    method: str
    path: str
    summary: str = ""
    description: str = ""
    responses: List[ApiResponse] = field(default_factory=list)

@dataclass
class ControllerInfo:
    name: str
    tag: str
    description: str
    endpoints: List[ApiEndpoint] = field(default_factory=list)

def extract_tag(content: str) -> str:
    """Extract @Tag annotation value"""
    match = re.search(r'@Tag\s*\(\s*name\s*=\s*"([^"]+)"', content)
    return match.group(1) if match else "Unknown"

def extract_class_description(content: str) -> str:
    """Extract class-level JavaDoc"""
    match = re.search(r'/\*\*\s*\n(.*?)\*/', content, re.DOTALL)
    if match:
        doc = match.group(1)
        lines = [l.strip().lstrip('*').strip() for l in doc.split('\n')]
        lines = [l for l in lines if l and not l.startswith('@')]
        return ' '.join(lines) if lines else ""
    return ""

def extract_endpoints(content: str) -> List[ApiEndpoint]:
    """Extract all endpoints from controller content"""
    endpoints = []

    # Find all @XxxMapping annotations - capture the actual method name with original case
    method_pattern = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(')
    method_matches = list(method_pattern.finditer(content))

    for i, match in enumerate(method_matches):
        method_original = match.group(1)  # Original case: Get, Post, etc.
        method_upper = method_original.upper()
        start = match.start()
        end = method_matches[i+1].start() if i+1 < len(method_matches) else len(content)
        method_block = content[start:end]

        # Extract path - use original method name for matching
        path_pattern = re.compile(r'@' + method_original + r'Mapping\s*\(\s*(?:value\s*)?=\s*"?([^")]+)"?\)?')
        path_match = path_pattern.search(method_block)
        if not path_match:
            # Try simpler pattern without value=
            path_pattern2 = re.compile(r'@' + method_original + r'Mapping\s*\(\s*"([^"]+)"')
            path_match = path_pattern2.search(method_block)
        if not path_match:
            continue
        path = path_match.group(1).strip()

        # Extract @Operation summary
        summary = ""
        op_match = re.search(r'@Operation\s*\([^}]*?summary\s*=\s*"([^"]+)"', method_block, re.DOTALL)
        if op_match:
            summary = op_match.group(1).strip()

        # Extract @Operation description
        description = ""
        desc_match = re.search(r'@Operation\s*\([^}]*?description\s*=\s*"([^"]+)"', method_block, re.DOTALL)
        if desc_match:
            description = desc_match.group(1).strip()
            if '\\n' in description:
                description = description.replace('\\n', ' ')

        # Extract @ApiResponses
        responses = []
        for resp_match in re.finditer(r'@ApiResponse\s*\([^)]*?responseCode\s*=\s*"([^"]+)"[^}]*?message\s*=\s*"([^"]+)"', method_block, re.DOTALL):
            responses.append(ApiResponse(
                code=resp_match.group(1).strip(),
                message=resp_match.group(2).strip()
            ))

        endpoints.append(ApiEndpoint(
            method=method_upper,
            path=path,
            summary=summary,
            description=description,
            responses=responses
        ))

    return endpoints

def scan_service(base_path: Path, service_name: str, controller_rel_path: str) -> List[ControllerInfo]:
    """Scan all controllers in a service"""
    controllers = []
    controller_dir = base_path / controller_rel_path

    if not controller_dir.exists():
        return controllers

    for controller_file in controller_dir.glob('*Controller.java'):
        if controller_file.name == 'HealthController.java':
            continue

        content = controller_file.read_text(encoding='utf-8')
        tag = extract_tag(content)
        description = extract_class_description(content)
        endpoints = extract_endpoints(content)

        if endpoints:
            controllers.append(ControllerInfo(
                name=controller_file.stem,
                tag=tag,
                description=description,
                endpoints=endpoints
            ))

    return controllers

def generate_markdown(controllers: List[ControllerInfo], port_map: Dict[str, str]) -> str:
    """Generate complete Markdown documentation"""
    lines = []
    lines.append("# FISCO BCOS 供应链金融平台 API 文档\n\n")
    lines.append("> 自动生成自 Swagger 注解 | 更新时间: 2026-03-30\n\n")

    # Summary table
    lines.append("## 服务概览\n\n")
    lines.append("| 服务 | 端口 | 控制器数 | 接口数 |\n")
    lines.append("|------|------|----------|--------|\n")

    by_tag = {}
    for ctrl in controllers:
        if ctrl.tag not in by_tag:
            by_tag[ctrl.tag] = []
        by_tag[ctrl.tag].append(ctrl)

    total_ctrls = 0
    total_eps = 0
    for tag, ctrls in sorted(by_tag.items(), key=lambda x: x[0]):
        port = port_map.get(tag, '-')
        ctrl_count = len(ctrls)
        ep_count = sum(len(c.endpoints) for c in ctrls)
        total_ctrls += ctrl_count
        total_eps += ep_count
        lines.append(f"| {tag} | {port} | {ctrl_count} | {ep_count} |\n")

    lines.append(f"| **合计** | - | **{total_ctrls}** | **{total_eps}** |\n\n")

    # Detailed docs by service
    for tag, service_controllers in sorted(by_tag.items(), key=lambda x: x[0]):
        lines.append(f"## {tag}\n\n")

        for ctrl in service_controllers:
            lines.append(f"### {ctrl.name}\n")
            if ctrl.description:
                lines.append(f"{ctrl.description}\n\n")
            else:
                lines.append("\n")

            # Table format
            lines.append("| 方法 | 路径 | 描述 |\n")
            lines.append("|------|------|------|\n")
            for ep in ctrl.endpoints:
                desc = ep.summary.replace('\\n', ' ').replace('\n', ' ').strip() if ep.summary else ''
                lines.append(f"| `{ep.method}` | `{ep.path}` | {desc} |\n")
            lines.append("\n")

            # Detailed section
            for ep in ctrl.endpoints:
                lines.append(f"#### `{ep.method} {ep.path}`\n")
                if ep.summary:
                    lines.append(f"**摘要**: {ep.summary}\n\n")
                if ep.description:
                    desc_clean = ep.description.replace('\\n', '\n').strip()
                    lines.append(f"{desc_clean}\n\n")
                if ep.responses:
                    lines.append("**响应码**:\n")
                    for resp in ep.responses:
                        lines.append(f"- `{resp.code}`: {resp.message}\n")
                    lines.append("\n")
                lines.append("---\n\n")

    return ''.join(lines)

def generate_per_service_docs(controllers: List[ControllerInfo], output_dir: Path):
    """Generate per-service markdown files"""
    by_tag = {}
    for ctrl in controllers:
        if ctrl.tag not in by_tag:
            by_tag[ctrl.tag] = []
        by_tag[ctrl.tag].append(ctrl)

    for tag, service_controllers in sorted(by_tag.items()):
        lines = []
        lines.append(f"# {tag} API 文档\n\n")

        for ctrl in service_controllers:
            lines.append(f"## {ctrl.name}\n\n")
            if ctrl.description:
                lines.append(f"{ctrl.description}\n\n")

            for ep in ctrl.endpoints:
                lines.append(f"### `{ep.method} {ep.path}`\n")
                if ep.summary:
                    lines.append(f"**摘要**: {ep.summary}\n\n")
                if ep.description:
                    desc_clean = ep.description.replace('\\n', '\n').strip()
                    lines.append(f"{desc_clean}\n\n")
                if ep.responses:
                    lines.append("**响应码**:\n")
                    for resp in ep.responses:
                        lines.append(f"- `{resp.code}`: {resp.message}\n")
                    lines.append("\n")
                lines.append("---\n\n")

        filename = tag.replace(' ', '_') + '.md'
        (output_dir / filename).write_text(''.join(lines), encoding='utf-8')
        print(f"Generated: {output_dir / filename}")

def main():
    base_path = Path('/home/llm_rca/fisco/supply-chain-finance')

    services = {
        'auth-service': ('services/auth-service/src/main/java/com/fisco/app/controller', '认证服务'),
        'enterprise-service': ('services/enterprise-service/src/main/java/com/fisco/app/controller', '企业服务'),
        'warehouse-service': ('services/warehouse-service/src/main/java/com/fisco/app/controller', '仓储服务'),
        'logistics-service': ('services/logistics-service/src/main/java/com/fisco/app/controller', '物流服务'),
        'finance-service': ('services/finance-service/src/main/java/com/fisco/app/controller', '金融服务'),
        'credit-service': ('services/credit-service/src/main/java/com/fisco/app/controller', '信用服务'),
        'fisco-gateway-service': ('services/fisco-gateway-service/src/main/java/com/fisco/app/controller', '区块链网关'),
    }

    port_map = {
        '用户认证': '8081',
        '用户管理': '8081',
        '登出': '8081',
        '企业管理': '8082',
        '仓单管理': '8083',
        '物流管理': '8084',
        '金融管理': '8085',
        '质押贷款管理': '8085',
        '信用管理': '8086',
        '区块链基础服务': '8087',
        '区块链业务服务': '8087',
    }

    all_controllers = []
    for service_name, (rel_path, _) in services.items():
        controllers = scan_service(base_path, service_name, rel_path)
        all_controllers.extend(controllers)

    output_dir = base_path / 'docs' / 'api'
    output_dir.mkdir(parents=True, exist_ok=True)

    # Generate full documentation
    full_doc = generate_markdown(all_controllers, port_map)
    (output_dir / 'API_DOCUMENTATION.md').write_text(full_doc, encoding='utf-8')
    print(f"Generated: {output_dir / 'API_DOCUMENTATION.md'}")

    # Generate per-service docs
    generate_per_service_docs(all_controllers, output_dir)

    # Generate summary only
    summary_lines = ["# API 文档摘要\n\n"]
    summary_lines.append("| 服务 | 端口 | 接口数 |\n")
    summary_lines.append("|------|------|--------|\n")

    by_tag = {}
    for ctrl in all_controllers:
        if ctrl.tag not in by_tag:
            by_tag[ctrl.tag] = []
        by_tag[ctrl.tag].append(ctrl)

    for tag, ctrls in sorted(by_tag.items(), key=lambda x: x[0]):
        port = port_map.get(tag, '-')
        ep_count = sum(len(c.endpoints) for c in ctrls)
        summary_lines.append(f"| {tag} | {port} | {ep_count} |\n")

    (output_dir / 'SUMMARY.md').write_text(''.join(summary_lines), encoding='utf-8')
    print(f"Generated: {output_dir / 'SUMMARY.md'}")

    print(f"\n文档生成完成！共 {len(all_controllers)} 个控制器, {sum(len(c.endpoints) for c in all_controllers)} 个接口")

if __name__ == '__main__':
    main()
