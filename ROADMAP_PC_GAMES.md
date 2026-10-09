# CloudyPlay — jogos de PC no Android

## Etapas

1. Biblioteca local com importação por seletor de documentos Android e persistência das referências de arquivos.
2. Integração do motor de compatibilidade Windows x64: Wine, Box64 e bibliotecas ARM64.
3. Testes gráficos na GPU Mali do Galaxy M22; não presumir suporte a drivers específicos de GPUs Adreno.
4. Integrações de lojas Steam, Epic Games Store e GOG usando fluxos de autenticação e download permitidos por cada plataforma.
5. Compilar via GitHub Actions e testar com um aplicativo Windows x64 simples antes de avaliar jogos 3D.

## Limitações conhecidas

- O launcher Android/ARM64 não executa arquivos Windows x64 sozinho. Wine, Box64, bibliotecas compatíveis e uma camada gráfica funcional são necessários.
- Importar um instalador não equivale a instalar ou executar o jogo.
- A integração de uma loja não garante compatibilidade com DRM, anticheat ou todos os títulos.
- Não adicionar credenciais privadas de loja nem chaves secretas do Supabase ao aplicativo.
- Verificar licenças de cada componente de terceiros antes de redistribuir binários.

O login administrativo Supabase existente deve ser preservado durante as alterações.