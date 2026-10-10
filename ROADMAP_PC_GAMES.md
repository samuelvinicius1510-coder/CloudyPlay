# CloudyPlay — jogos de PC no Android

## Etapas

1. **Biblioteca local:** selecionar e copiar arquivos para o armazenamento privado do app. Concluído como preparação de arquivos; isso não instala nem executa os arquivos.
2. **Preparar integração do motor:** o repositório oficial Winlator é um aplicativo Android completo e contém componentes em repositórios/submódulos separados; não é uma dependência Gradle que possa ser adicionada ao CloudyPlay com segurança. Referência: https://github.com/brunodev85/winlator
3. **Integrar fonte e dependências nativas:** escolher uma versão de origem, fixar revisões dos submódulos, revisar licenças e compilar componentes para arm64-v8a. Não copiar binários ou rootfs sem confirmar as licenças e a origem.
4. **Camada gráfica:** avaliar Vulkan/OpenGL e tradução gráfica compatível com a GPU Mali do Galaxy M22; não presumir suporte a drivers específicos de GPUs Adreno.
5. **Inicialização real:** preparar o root filesystem/prefixo Wine, integração JNI e um teste de execução de um programa Windows simples.
6. **Biblioteca e jogos:** conectar o botão Executar ao motor integrado somente após o teste de inicialização funcionar; depois avaliar jogos e limitações de DRM/anticheat.
7. **Lojas:** integrar Steam, Epic Games Store e GOG apenas por fluxos oficiais/autorizados e suportados por cada plataforma.

## Preparação de CI

O workflow de APK agora faz checkout recursivo de submódulos, para que dependências adicionadas como submódulos Git não sejam omitidas silenciosamente. Isso é apenas preparação do pipeline; não adiciona o Winlator ao aplicativo nem garante que seus componentes compilarão no CloudyPlay.

## Estado atual honesto

- O CloudyPlay **ainda não contém um motor Windows integrado** e ainda não executa jogos de PC sozinho.
- A biblioteca copia arquivos selecionados para o armazenamento privado do aplicativo, preparando o acesso futuro pelo motor nativo.
- O launcher Android/ARM64 não executa arquivos Windows x64 sozinho. São necessários Wine, uma camada de tradução x86/x64 para ARM64, bibliotecas compatíveis e uma camada gráfica funcional.
- Um APK de 1–2 GB é possível em princípio, mas o tamanho final depende dos componentes e arquiteturas incluídos; tamanho não garante compatibilidade nem desempenho.
- Não adicionar credenciais privadas de loja nem chaves secretas do Supabase ao aplicativo.
- Verificar as licenças de cada componente antes de redistribuir código ou binários.
- O login administrativo Supabase existente deve ser preservado durante as alterações.
