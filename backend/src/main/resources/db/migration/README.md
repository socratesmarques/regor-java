# Migrations do Regor

Adicione migrations SQL versionadas no formato `V1__descricao.sql` ao definir o primeiro modelo de domínio.
Não há tabelas de negócio nesta etapa. Não edite migrations já aplicadas.
Flyway gerencia o esquema; Hibernate apenas valida os mapeamentos existentes.
