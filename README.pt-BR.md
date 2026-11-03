<div align="center">

<img src="art/social-preview-1280x640.png" alt="Janelas de projeto soltas viram abas numa janela só" width="640">

<br>

<p>
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.4">
  <img src="https://img.shields.io/badge/IntelliJ_Platform-2026.1+-000000?style=for-the-badge&logo=jetbrains&logoColor=white" alt="IntelliJ Platform 2026.1 ou mais novo">
  <a href="https://gitlab.com/danie1net0/window-tabs/-/pipelines"><img src="https://img.shields.io/gitlab/pipeline-status/danie1net0%2Fwindow-tabs?branch=master&style=for-the-badge&label=CI" alt="Status do pipeline"></a>
  <a href="https://gitlab.com/danie1net0/window-tabs/-/pipelines"><img src="https://img.shields.io/gitlab/pipeline-coverage/danie1net0%2Fwindow-tabs?branch=master&style=for-the-badge&label=cobertura" alt="Cobertura dos testes medida no CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/licen%C3%A7a-MIT-F2C94C?style=for-the-badge" alt="Licença MIT"></a>
</p>

<sub><a href="README.md">🇺🇸 English</a> · <b>🇧🇷 Português</b></sub>

</div>

## Por quê

No macOS, os IDEs da JetBrains juntam as janelas de projeto em abas nativas. No Linux esse recurso não existe: cada
projeto abre numa janela própria, e trocar de projeto vira caça no Alt+Tab. O Window Tabs põe uma faixa de abas no topo
de cada janela do PhpStorm e do GoLand (qualquer IDE JetBrains 2026.1+ serve), uma aba por projeto aberto.

- **Clique numa aba**: a janela daquele projeto assume o tamanho e a posição da atual (ou fica maximizada, se a atual
  estiver) e vem para a frente.
- **Clique do meio ou `×`** (aparece no hover): fecha o projeto.
- **`+`** à direita: abre outro projeto.
- A faixa só aparece com dois ou mais projetos abertos.

Cada projeto continua sendo uma janela própria; o plugin só faz a troca parecer uma aba.

## Instalação

Baixe `window-tabs-<versão>.zip` dos artifacts do último
[job `build`](https://gitlab.com/danie1net0/window-tabs/-/pipelines), ou gere o zip (abaixo). No IDE:
**Settings → Plugins → ⚙ → Install Plugin from Disk…** e escolha o zip. Não precisa reiniciar.

## Linux / GNOME

- No Wayland, o GNOME pode recusar trazer a janela para a frente e mostrar só o aviso "PhpStorm is ready". Se
  acontecer, rode o IDE em X11: **Help → Edit Custom VM Options** → `-Dawt.toolkit.name=XToolkit`.
- Deixe as janelas maximizadas: a troca fica instantânea e sem salto de posição.
- O Alt+Tab do GNOME continua listando uma janela por projeto. O Super+` (*Switch windows of an application*) também
  alterna entre elas.

## Build

O Gradle baixa o IntelliJ IDEA 2026.1.5, a plataforma mais antiga suportada, e compila e testa contra ele. Qualquer
JDK 21+ roda o build; em máquina sem JDK, aponte o `JAVA_HOME` para o JBR do próprio IDE:

```bash
# macOS
export JAVA_HOME=/Applications/PhpStorm.app/Contents/jbr/Contents/Home
# Linux (Toolbox)
export JAVA_HOME=~/.local/share/JetBrains/Toolbox/apps/phpstorm/jbr
```

```bash
./gradlew buildPlugin
```

O zip sai em `build/distributions/`.

## Qualidade

```bash
./gradlew check
```

Roda tudo que o CI exige:

- **ktlint**: estilo oficial do Kotlin (`./gradlew ktlintFormat` corrige sozinho).
- **detekt**: análise estática, regras padrão mais `config/detekt.yml`.
- **Compilador**: qualquer aviso vira erro.
- **Testes**: de unidade e de plataforma (um IDE sem tela abrindo e fechando projetos reais).
- **Kover**: 100% das linhas e dos branches do plugin cobertos; abaixo disso, o build falha.

Compatibilidade com PhpStorm e GoLand (`verifyIdeVersion` em `gradle.properties`; baixa os dois IDEs):

```bash
./gradlew verifyPlugin
```

O pipeline do GitLab roda `ktlint` e `detekt`, depois `test` e `verify`, e por fim `build`, que guarda o zip como
artifact.

## Limitação

A faixa usa o extension point `ideRootPaneNorth`, marcado como interno pela JetBrains. O `verifyPlugin` aceita (só
falha por incompatibilidade real), mas uma versão futura do IDE pode mudar essa API.

## Repositório

O código vive em [gitlab.com/danie1net0/window-tabs](https://gitlab.com/danie1net0/window-tabs).
[github.com/danie1net0/window-tabs](https://github.com/danie1net0/window-tabs) é um espelho somente leitura.

## Licença

[MIT](LICENSE)
