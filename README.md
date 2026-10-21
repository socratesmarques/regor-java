# Regor

Base Java 21 para migração gradual. `legacy-web/` permanece intacto como referência visual e funcional; não faz parte do build Maven.

## Estrutura

```text
legacy-web/                  Referência React original
desktop/                     Aplicação JavaFX com FXML e CSS separado
  src/main/java/             Aplicação, controller, service e dto
  src/main/resources/        Telas FXML e estilos
backend/                     Spring Boot e APIs futuras
  src/main/java/             config, controller, service, repository, entity, dto e websocket
  src/main/resources/        Configuração e db/migration para Flyway
.agents/skills/              Orientações locais de desenvolvimento
pom.xml                      Agregador; os módulos também compilam separadamente
```

O desktop consome o backend por HTTP/REST e WebSocket, sem dependências JPA ou conexão direta ao banco. Os pacotes service/dto estão reservados para essa integração; ainda não há chamadas de rede.

O backend concentra regras de negócio e autorização. PostgreSQL armazena os dados, JPA fornece persistência e Flyway controla migrations. A configuração inicial nega todas as requisições; login, JWT, endpoints REST, canais de chat, entidades e regras de matches serão implementados nas próximas etapas. BCrypt já está disponível como bean. Não há credenciais embutidas.

## Compilar

Instale um JDK 21+ compatível e configure `JAVA_HOME`. O Maven Wrapper fixa o Maven 3.9.16 e baixa suas dependências no primeiro uso. A compilação não requer PostgreSQL em execução.

Na raiz, em PowerShell:

```powershell
.\mvnw.cmd -f desktop/pom.xml clean compile
.\mvnw.cmd -f backend/pom.xml clean compile
.\mvnw.cmd clean package
```

Em Linux/macOS, use `sh ./mvnw` no lugar de `.\mvnw.cmd`. Cada módulo também aceita Maven instalado (`mvn -f desktop/pom.xml clean compile`, por exemplo).

## Executar

Desktop:

```powershell
.\mvnw.cmd -f desktop/pom.xml javafx:run
```

A tela inicial usa as cores e a divisão lateral de `legacy-web/src/components/Sidebar.tsx` e `legacy-web/src/App.tsx`. A navegação e as telas funcionais ainda não foram migradas.

Backend: disponibilize um banco PostgreSQL chamado `regor` e configure variáveis no ambiente do processo. `application.yml` não carrega arquivos `.env` automaticamente.

```powershell
$env:REGOR_DB_URL = 'jdbc:postgresql://localhost:5432/regor'
$env:REGOR_DB_USERNAME = 'seu_usuario'
$env:REGOR_DB_PASSWORD = 'sua_senha_local'
.\mvnw.cmd -f backend/pom.xml spring-boot:run
```

`REGOR_SERVER_PORT` é opcional e tem valor padrão `8080`. Sem PostgreSQL acessível ou credenciais válidas, o backend falha na inicialização. Os pacotes de testes JUnit 5, Mockito e Spring Security Test estão disponíveis no backend; testes de regras de negócio acompanharão a implementação dessas regras.
