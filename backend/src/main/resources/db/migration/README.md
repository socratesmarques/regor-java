# Migrations do Regor

- V1 cria as dez tabelas do domínio, chaves estrangeiras, índices e constraints.
- V2 adiciona a versão de sessão para revogação de JWTs e valida participantes contra o match do chat.

Flyway aplica as migrations ao iniciar. Hibernate usa ddl-auto=validate.
Crie uma nova migration para evoluir o esquema; não altere migrations já aplicadas.
