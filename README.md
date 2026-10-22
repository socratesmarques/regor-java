# Regor — JavaFX + Spring Boot + PostgreSQL

Aplicação Java 21 com interface JavaFX/FXML/CSS, API REST, JWT e chat em tempo real. O projeto React em legacy-web/ é preservado como referência visual e funcional.

## Implementado

- Landing, login, cadastro, Home, descoberta, perfil e fotos, chats/matches, notificações e configurações.
- Likes, dislikes e superlikes persistentes; match recíproco transacional e sem duplicação sob concorrência.
- Chat disponível somente após match, histórico persistente e eventos WebSocket após commit.
- Descoberta por busca, idade, cidade, interesse, gênero e distância; preferências persistentes.
- Fotos verificadas no servidor, notificações por destinatário e controles de privacidade.
- BCrypt, JWT de uma hora, revogação após troca de senha, autorização por recurso e proteção contra mass assignment.

A interface usa terracota (#C73532) nas ações, salmão (#F0A39B) nos detalhes, marfim (#FFFEFC) no conteúdo e marrom (#492326) nos textos. Ícones vetoriais seguem uma grade única. A navegação pode usar Marrom ou Marfim nas configurações de aparência. O perfil reúne fotos, informações e interesses com prévia ao vivo, salvamento fixo, descarte de edição e proteção ao navegar com alterações pendentes. Não carrega mocks nem Firebase nas funcionalidades migradas. Reels, Stories, pagamentos, chamadas de voz/vídeo e importação de dados antigos não fazem parte desta migração.

## Organização

- desktop/: aplicação, navegação, controllers pequenos e serviços HTTP/WebSocket. Sem acesso direto ao banco.
- backend/: controller, service, repository, entity, dto, config e websocket.
- backend/src/main/resources/db/migration/: V1 com as dez tabelas e V2 com revogação de sessão/validação de participantes.
- .agents/skills/: orientações locais.
- docs/API.md: contratos da API e comportamento de autorização.
- compose.yml: PostgreSQL opcional para desenvolvimento, na porta local 5433.
- scripts/test-ui.ps1: teste conectado com PostgreSQL e dados temporários, sem alterar um banco existente.

## Compilar e testar

Requer JDK 21+ compatível com JavaFX 21. Configure JAVA_HOME. Maven Wrapper incluído.

Na raiz, em PowerShell:

```powershell
.\mvnw.cmd -f backend/pom.xml clean test
.\mvnw.cmd -f desktop/pom.xml clean test
.\mvnw.cmd clean verify
```

Os testes do backend iniciam PostgreSQL temporário real via embedded-postgres; não usam H2 nem exigem credenciais do seu banco. As primeiras execuções baixam Maven, dependências e os binários de teste.

Os testes JavaFX exigem ambiente gráfico; em Linux CI, execute com Xvfb. O teste de renderização grava imagens PPM em desktop/target/screenshots/. Os relatórios JUnit ficam em target/surefire-reports de cada módulo. Os dados sintéticos existem somente em src/test/.

## Executar localmente

Use um PostgreSQL existente ou inicie o serviço opcional:

```powershell
$env:REGOR_DB_PASSWORD = 'defina-uma-senha-local'
docker compose up -d postgres
$env:REGOR_DB_URL = 'jdbc:postgresql://localhost:5433/regor'
$env:REGOR_DB_USERNAME = 'regor'
$env:REGOR_JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd -f backend/pom.xml spring-boot:run
```

Mantenha REGOR_JWT_SECRET estável entre reinicializações para preservar sessões válidas. Não o versione. REGOR_SERVER_PORT é opcional, com padrão 8080. O backend não lê arquivos .env automaticamente: exporte as variáveis no ambiente. O Compose pode ler .env.

Em outro terminal:

```powershell
$env:REGOR_API_URL = 'http://localhost:8080'
.\mvnw.cmd -f desktop/pom.xml javafx:run
```

Para um servidor remoto, use HTTPS. O banco começa vazio: cadastre duas contas, complete os perfis, envie fotos e faça curtidas recíprocas para habilitar uma conversa. Ao trocar a senha, entre novamente.

Os JARs são gerados em backend/target e desktop/target. O backend é executável com java -jar; para o desktop use javafx:run, que configura o module path e as bibliotecas nativas.

## Teste conectado da interface

```powershell
.\scripts\test-ui.ps1
```

O script inicia um backend em porta aleatória com PostgreSQL descartável, cria contas e conexões pelas regras reais, testa login/cadastro pelas telas JavaFX e renderiza as telas autenticadas com dados. Ao terminar, encerra o servidor temporário. Não acessa dados do PostgreSQL existente.

## Limites operacionais

A autenticação é própria do novo backend; contas/dados Firebase não são importados automaticamente. O chat WebSocket está preparado para uma instância do servidor. Contas VIP, moedas e verificação são administradas no backend e não possuem fluxo de compra ou concessão pública nesta entrega.
