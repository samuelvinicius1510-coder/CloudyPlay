# CloudyPlay — jogos de PC no Android

## Etapas

1. **Biblioteca local:** selecionar e copiar arquivos para o armazenamento privado do app. Concluído como preparação de arquivos; referências antigas de documentos continuam sendo reconhecidas.
2. **Motor integrado:** integrar componentes de compatibilidade Windows/ARM64 (Wine, Box64 e bibliotecas nativas) com uma estratégia de empacotamento e licenças verificadas. Em andamento; ainda não integrado.
3. **Camada gráfica:** avaliar Vulkan/OpenGL e tradução gráfica compatível com a GPU Mali do Galaxy M22, sem presumir suporte a drivers específicos de GPUs Adreno.
4. **Instalação e execução:** conectar a biblioteca ao prefixo do Wine, criar ambientes por jogo e iniciar executáveis pelo motor integrado.
5. **Testes reais:** compilar via GitHub Actions e testar primeiro um aplicativo Windows x64 simples; só depois avaliar jogos 3D e DRM/anticheat.
6. **Lojas:** integrar Steam, Epic Games Store e GOG apenas por fluxos oficiais/autorizados e suportados por cada plataforma.

## Estado atual honesto

- O CloudyPlay **ainda não contém um motor Windows integrado** e ainda não executa jogos de PC sozinho.
- A biblioteca agora copia arquivos selecionados para o armazenamento privado do aplicativo, preparando o acesso futuro pelo motor nativo. Isso não instala nem executa os arquivos.
- O botão “Executar” informa esse estado para os arquivos copiados internamente; não afirma que um jogo foi iniciado.
- O launcher Android/ARM64 não executa arquivos Windows x64 sozinho. São necessários Wine, uma camada de tradução x86/x64 para ARM64, bibliotecas compatíveis e uma camada gráfica funcional.
- Um APK de 1–2 GB é possível em princípio, mas o tamanho final depende de quais componentes e arquiteturas forem incluídos; tamanho não garante compatibilidade nem desempenho.
- Não adicionar credenciais privadas de loja nem chaves secretas do Supabase ao aplicativo.
- Verificar licenças de cada componente de terceiros antes de redistribuir binários.

O login administrativo Supabase existente deve ser preservado durante as alterações.
