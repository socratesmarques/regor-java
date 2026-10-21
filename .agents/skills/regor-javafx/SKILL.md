---
name: regor-javafx
description: Desenvolver ou revisar a interface desktop do Regor em JavaFX, usando o projeto React atual como referência visual.
---

# Regor JavaFX

- Use Java 21+, JavaFX e Maven.
- Defina as telas em FXML e mantenha o CSS em arquivos separados.
- Mantenha controllers pequenos, focados em eventos e estado da interface; extraia integração e lógica para serviços.
- Não acesse o banco diretamente. Consuma o backend via HTTP/REST e WebSocket para chat.
- Execute chamadas de rede fora da JavaFX Application Thread e atualize a interface nessa thread.
- Consulte telas, componentes e estilos do projeto React atual antes de implementar a interface; se a referência estiver ausente, solicite sua localização.
- Preserve layout, cores, navegação e experiência visual, incluindo estados de carregamento, erro e vazio.
