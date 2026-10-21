---
name: regor-database
description: Modelar ou revisar persistência, relacionamentos, constraints e migrations PostgreSQL do Regor.
---

# Regor Database

- Use PostgreSQL com Spring Data JPA/Hibernate.
- Gerencie o esquema com Flyway e migrations versionadas em src/main/resources/db/migration, no formato V<versao>__<descricao>.sql.
- Crie uma nova migration para cada evolução; não edite migrations já aplicadas nem use atualização automática do Hibernate como substituto.
- Defina relacionamentos, cardinalidades, chaves estrangeiras, NOT NULL, UNIQUE e CHECK conforme as regras do domínio.
- Escolha cascatas e comportamento de exclusão explicitamente; adicione índices conforme consultas e relacionamentos.
- Proteja invariantes contra concorrência com transações e constraints adequadas, incluindo unicidade de matches conforme as regras definidas.
- Mantenha regras importantes no backend e reforce a integridade no banco; nunca dependa apenas de validação no cliente.
