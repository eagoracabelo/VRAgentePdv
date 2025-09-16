# Salt-Minion Universal Installer

Um instalador universal para o Salt-Minion desenvolvido em Kotlin Native, compatível com Linux e Windows.

## Características

- **Multiplataforma**: Suporte completo para Linux e Windows
- **Executável Nativo**: Sem dependência de JVM
- **Instalação Interativa**: Interface de linha de comando amigável
- **Instalação Silenciosa**: Automação via parâmetros
- **Gerenciamento de Versões**: Download automático de versões específicas
- **Validação de Conectividade**: Teste de conexão com Salt Master
- **Configuração Automática**: Geração de arquivos de configuração
- **Gerenciamento de Serviços**: Criação e controle de serviços do sistema
- **Backup Automático**: Backup de configurações existentes

## Estrutura do Projeto

```
salt-installer/
├── build.gradle.kts                 # Configuração do build
├── src/
│   ├── commonMain/kotlin/           # Código comum entre plataformas
│   │   ├── Main.kt                  # Ponto de entrada principal
│   │   ├── core/                    # Componentes core
│   │   │   ├── Platform.kt          # Detecção de plataforma
│   │   │   ├── Logger.kt            # Sistema de logging
│   │   │   ├── FileUtils.kt         # Utilitários de arquivo
│   │   │   ├── ProcessUtils.kt      # Execução de processos
│   │   │   └── DownloadUtils.kt     # Downloads e checksums
│   │   ├── version/                 # Gerenciamento de versões
│   │   │   └── SaltMinionVersionManager.kt
│   │   ├── network/                 # Configuração de rede
│   │   │   └── NetworkConfigurator.kt
│   │   ├── config/                  # Geração de configuração
│   │   │   └── ConfigurationGenerator.kt
│   │   └── platform/                # Interface de sistema
│   │       ├── PlatformDetector.kt
│   │       └── SystemManager.kt
│   ├── linuxMain/kotlin/            # Implementações específicas Linux
│   │   ├── core/
│   │   └── platform/
│   └── windowsMain/kotlin/          # Implementações específicas Windows
│       ├── core/
│       └── platform/
├── README.md
└── LICENSE
```

## Compilação

### Pré-requisitos

- Kotlin 1.9.20+
- Gradle 8.0+
- Para Linux: GCC/Clang
- Para Windows: Microsoft Visual C++ ou MinGW-w64

### Build

```bash
# Build para todas as plataformas
./gradlew buildAll

# Build apenas para Linux
./gradlew linuxX64Binaries

# Build apenas para Windows
./gradlew mingwX64Binaries

# Criar pacote de release
./gradlew packageRelease
```

### Executáveis Gerados

- **Linux**: `build/bin/linux/releaseExecutable/salt-installer-linux.kexe`
- **Windows**: `build/bin/windows/releaseExecutable/salt-installer-windows.exe`

## Uso

### Instalação Interativa

```bash
# Linux
./salt-installer-linux.kexe

# Windows
salt-installer-windows.exe
```

O instalador irá guiá-lo através das seguintes etapas:
1. Detecção automática da plataforma
2. Verificação de instalações existentes
3. Seleção de versão do Salt-Minion
4. Configuração do Salt Master (IP e porta)
5. Definição do Minion ID
6. Validação de conectividade
7. Download e instalação
8. Configuração e inicialização do serviço

### Instalação Silenciosa

```bash
# Instalação básica
./salt-installer --silent --master 192.168.1.100

# Instalação com parâmetros customizados
./salt-installer --silent \
  --master 192.168.1.100 \
  --port 4506 \
  --minion-id web-server-01 \
  --version 3006.4
```

### Outros Comandos

```bash
# Verificar status da instalação
./salt-installer --check

# Desinstalar Salt-Minion
./salt-installer --uninstall

# Exibir ajuda
./salt-installer --help

# Exibir versão
./salt-installer --version
```

## Parâmetros da Instalação Silenciosa

| Parâmetro | Descrição | Padrão |
|-----------|-----------|---------|
| `--master <ip>` | IP do Salt Master | `127.0.0.1` |
| `--port <porta>` | Porta do Salt Master | `4506` |
| `--minion-id <id>` | ID do Minion | Auto-gerado |
| `--version <versão>` | Versão do Salt a instalar | `latest` |

## Funcionalidades Detalhadas

### Detecção de Plataforma

O instalador detecta automaticamente:
- **Linux**: Distribuição (Ubuntu, CentOS, RHEL, Debian, etc.)
- **Windows**: Versão e arquitetura
- **Geral**: Nome do host, arquitetura, privilégios administrativos

### Gerenciamento de Versões

- Download automático de versões disponíveis
- Verificação de integridade com checksums
- Suporte a múltiplas distribuições Linux
- Cache local de downloads

### Configuração Automática

Gera automaticamente:
- **Arquivo principal**: `/etc/salt/minion` (Linux) ou `C:\salt\conf\minion` (Windows)
- **Configuração de logging**: Rotação automática de logs
- **Estrutura PKI**: Diretórios para chaves e certificados
- **Arquivos de serviço**: Systemd (Linux) ou Windows Service

### Exemplo de Configuração Gerada

```yaml
# Salt Minion Configuration
master: 192.168.1.100
master_port: 4506
id: web-server-01

# Logging
log_level: warning
log_file: /var/log/salt/minion

# Security
auto_accept_grains: False
verify_master_pubkey_sign: False

# Performance
multiprocessing: True
tcp_keepalive: True
```

### Validação de Conectividade

O instalador testa a conectividade com o Salt Master usando:
- Conexão TCP direta na porta configurada
- Ping ICMP para verificar alcançabilidade
- Múltiplos métodos de fallback (netcat, telnet)

## Logs

O instalador gera logs detalhados em `salt-installer.log` com:
- Timestamps de todas as operações
- Detecção de plataforma e configuração
- Progresso de downloads
- Erros e warnings
- Status de serviços

### Níveis de Log

- **DEBUG**: Informações detalhadas para troubleshooting
- **INFO**: Progresso normal da instalação
- **WARN**: Avisos não-críticos
- **ERROR**: Erros que impedem a instalação

## Backup e Recuperação

### Backup Automático

Antes de sobrescrever uma instalação existente, o instalador:
- Cria backup em diretório com timestamp
- Preserva configurações personalizadas
- Backup de logs recentes
- Lista localização do backup

### Localização dos Backups

- **Linux**: `/tmp/salt-backup-<timestamp>`
- **Windows**: `C:\temp\salt-backup-<timestamp>`

## Troubleshooting

### Problemas Comuns

**Erro: "Platform not supported"**
```bash
# Verifique a detecção de plataforma
./salt-installer --check
```

**Erro: "Cannot connect to Salt Master"**
```bash
# Teste conectividade manualmente
ping <master-ip>
telnet <master-ip> 4506
```

**Erro: "Permission denied"**
```bash
# Execute com privilégios administrativos
sudo ./salt-installer        # Linux
# Executar como Administrador # Windows
```

**Serviço não inicia**
```bash
# Linux
sudo systemctl status salt-minion
sudo journalctl -u salt-minion

# Windows
sc query salt-minion
```

### Logs de Debug

Para habilitar logs detalhados, defina a variável de ambiente:
```bash
export SALT_INSTALLER_LOG_LEVEL=DEBUG
./salt-installer
```

## Arquivos de Configuração

### Linux

- **Configuração**: `/etc/salt/minion`
- **Logs**: `/var/log/salt/minion`
- **Cache**: `/var/cache/salt/minion`
- **PKI**: `/etc/salt/pki/minion`
- **Serviço**: `/etc/systemd/system/salt-minion.service`

### Windows

- **Configuração**: `C:\salt\conf\minion`
- **Logs**: `C:\salt\var\log\salt\minion`
- **Cache**: `C:\salt\var\cache\salt\minion`
- **PKI**: `C:\salt\conf\pki\minion`
- **Serviço**: Windows Service Manager

## Segurança

### Considerações de Segurança

- Execução com privilégios mínimos necessários
- Validação de checksums de downloads
- Configurações PKI seguras (permissões 700)
- Logs sem informações sensíveis
- Backup seguro de configurações

### Permissões de Arquivos

O instalador configura automaticamente as permissões apropriadas:
- Configurações: Legível pelo usuário root/sistema
- PKI: Acesso restrito (700)
- Logs: Gravável pelo serviço Salt

## Contribuição

### Desenvolvimento

1. Clone o repositório
2. Configure o ambiente Kotlin Native
3. Implemente modificações nas pastas apropriadas:
   - `commonMain`: Funcionalidades comuns
   - `linuxMain`: Específico Linux
   - `windowsMain`: Específico Windows
4. Teste em ambas as plataformas
5. Submit pull request

### Testes

```bash
# Executar testes unitários
./gradlew test

# Teste manual de instalação
./gradlew build
./build/bin/linux/debugExecutable/salt-installer-linux.kexe --check
```

## Licença

Este projeto está licenciado sob a licença Apache License v2 - veja o arquivo [LICENSE](LICENSE) para detalhes.

## Suporte

Para suporte e relatório de bugs:
- Abra uma issue no repositório
- Inclua logs relevantes (`salt-installer.log`)
- Especifique plataforma e versão
- Descreva passos para reproduzir o problema

---

**Nota**: Este instalador é uma ferramenta independente e não é oficialmente mantida pelo projeto SaltStack. Para suporte oficial do Salt, consulte a [documentação oficial](https://docs.saltproject.io/).