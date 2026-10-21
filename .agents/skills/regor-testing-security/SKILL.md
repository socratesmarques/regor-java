---
name: regor-testing-security
description: Criar ou revisar testes de regras de negócio e segurança do Regor, especialmente autenticação, chats, matches e campos privilegiados.
---

# Regor Testing Security

- Use JUnit 5 e Mockito para testes unitários de regras de negócio; complemente com testes de integração de segurança quando necessário.
- Cubra sucesso, entradas inválidas, limites e tentativas de acesso indevido; teste comportamento observável.
- Teste autenticação e autorização, incluindo JWT ausente, inválido ou expirado e acesso a recursos de outro usuário.
- Verifique que somente participantes autorizados leem chats, assinam canais e enviam mensagens por REST/WebSocket.
- Teste criação de matches conforme as regras do domínio, incluindo tentativas não autorizadas, duplicadas e concorrentes.
- Envie payloads maliciosos em cadastro e atualização tentando atribuir role, VIP, verified, coins e permissões; confirme que o estado privilegiado permanece inalterado.
- Impeça mass assignment com DTOs de campos permitidos e mapeamento explícito; teste operações privilegiadas separadamente com autorização no servidor.
- Execute os testes Maven pertinentes e informe resultados e limitações; não declare como executado o que não foi rodado.
