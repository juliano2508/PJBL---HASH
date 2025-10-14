# Análise de Desempenho de Tabelas Hash

## Integrantes do Grupo
- Emmanuel Antonietti Ribeiro dos Santos
- Juliano Cesar Enns Miranda Marcos
- Vinícius Paraíso Dias


---

## 1. Introdução

Este trabalho tem como objetivo implementar e realizar uma análise comparativa de desempenho de diferentes algoritmos e estratégias de tratamento de colisão para Tabelas Hash em Java. Foram implementadas três abordagens distintas: **Encadeamento Separado com Inserção Ordenada**, **Endereçamento Aberto com Sondagem Quadrática** e **Endereçamento Aberto com Hash Duplo**.

As implementações foram submetidas a testes com três conjuntos de dados de tamanhos variados (100 mil, 1 milhão e 10 milhões de registros) e três tamanhos de tabela, permitindo uma análise detalhada sobre o impacto do fator de carga (`α`) no desempenho de cada método. As métricas coletadas incluem tempo de inserção, tempo de busca, número de colisões, distribuição das listas encadeadas e análise de gaps entre os elementos.

## 2. Metodologia e Implementação

### 2.1. Ambiente de Desenvolvimento
* **Linguagem:** Java
* **Versão do JDK:** [Ex: 11]
* **Sistema Operacional:** [Ex: Windows 11]
* **IDE:** [Ex: VS Code, IntelliJ]

### 2.2. Geração de Dados
Os conjuntos de dados foram gerados de forma programática para garantir a reprodutibilidade dos testes.
* **Tamanhos dos Datasets:** 100.000, 1.000.000 e 10.000.000 de registros.
* **Formato do Registro:** Código de 9 dígitos (`String` formatada como "000000000" a "999999999").
* **Seed Aleatória:** Foi utilizada uma `seed` fixa (`123456789L`) para o gerador de números aleatórios, garantindo que os mesmos conjuntos de dados fossem usados em todos os testes.

### 2.3. Tamanhos da Tabela Hash
Foram escolhidos três tamanhos para o vetor da tabela, com variação de aproximadamente 10x entre eles. Optou-se por números primos para melhorar a distribuição das chaves, especialmente no hashing por divisão.
* **Tamanho 1:** 1.009
* **Tamanho 2:** 10.007
* **Tamanho 3:** 100.003

### 2.4. Estratégias Implementadas

Foram implementadas três estratégias distintas para o tratamento de colisões.

#### Estratégia 1: Encadeamento Separado (com Inserção Ordenada)
Nesta abordagem, cada posição da tabela hash aponta para uma lista ligada ("balde"). Quando ocorre uma colisão, o novo registro é adicionado à lista correspondente.
* **Função Hash Principal:** Foi utilizado o **Método da Multiplicação** (sugerido por Knuth), que calcula o índice como `hash = floor(M * (k * A mod 1))`. Esta função é conhecida por sua boa distribuição, independentemente da escolha do tamanho `M`.
* **Detalhe de Implementação:** Os elementos foram inseridos de forma **ordenada** dentro de cada lista ligada. Essa escolha pode aumentar ligeiramente o custo de inserção, mas pode, em teoria, otimizar buscas em listas muito longas (embora o impacto seja mínimo se a distribuição for boa).

#### Estratégia 2: Endereçamento Aberto (Sondagem Quadrática)
A sondagem quadrática é uma estratégia de endereçamento aberto que busca resolver colisões tentando posições progressivamente mais distantes da original, seguindo uma função quadrática, o que ajuda a mitigar o problema de agrupamento primário.
* **Função de Sondagem:** `h(k, i) = (h1(k) + c1*i + c2*i²) mod M`. No código, foram usados os coeficientes `c1=1` e `c2=3`, resultando na fórmula `pos = (h1 + i + 3*i*i) mod M`.
* **Função Hash Principal (h1):** Também utiliza o Método da Multiplicação.

#### Estratégia 3: Endereçamento Aberto (Hash Duplo)
O Hash Duplo é considerado uma das técnicas mais robustas de endereçamento aberto, pois utiliza uma segunda função hash para calcular o "passo" da sondagem. Isso gera uma sequência de sondagem única para cada chave, eliminando os problemas de agrupamento.
* **Função Hash Principal (h1):** Método da Multiplicação.
* **Função Hash Secundária (h2):** A função `h2` foi implementada invertendo os dígitos da chave e aplicando o módulo `(M-1)`. A fórmula final é `h2(k) = 1 + (inverter(k) % (M-1))`, que garante que o passo nunca seja zero.

---

## 3. Resultados

Os testes foram executados e os resultados foram compilados no arquivo `resultados.csv`. As tabelas e gráficos abaixo resumem as principais métricas de desempenho.

### 3.1. Tabelas de Desempenho

**Como fazer:** Abra o arquivo `resultados.csv` que o programa gerou. Você pode usar o Excel ou Google Sheets. Copie os dados e use um "Markdown Table Generator" online ou formate manualmente como no exemplo abaixo. **Crie uma tabela para cada métrica importante (tempo, colisões, etc.).**

**Exemplo: Tabela de Tempo de Inserção (em milissegundos)**

| Tamanho Dataset | Tamanho Tabela | Fator de Carga (α) | Encadeamento Ordenado | Sondagem Quadrática | Hash Duplo |
|:---------------:|:--------------:|:------------------:|:---------------------:|:-------------------:|:----------:|
| 100.000         | 10.007         | ~10.0              | [Tempo em ms]         | [Tempo em ms]       | [Tempo em ms] |
| 100.000         | 100.003        | ~1.0               | [Tempo em ms]         | [Tempo em ms]       | [Tempo em ms] |
| 1.000.000       | 100.003        | ~10.0              | [Tempo em ms]         | [Tempo em ms]       | [Tempo em ms] |
| ...             | ...            | ...                | ...                   | ...                 | ...        |

**Exemplo: Tabela de Colisões**

| Tamanho Dataset | Tamanho Tabela | Fator de Carga (α) | Encadeamento Ordenado | Sondagem Quadrática | Hash Duplo |
|:---------------:|:--------------:|:------------------:|:---------------------:|:-------------------:|:----------:|
| 100.000         | 10.007         | ~10.0              | [Nº de Colisões]      | [Nº de Colisões]    | [Nº de Colisões] |
| 100.000         | 100.003        | ~1.0               | [Nº de Colisões]      | [Nº de Colisões]    | [Nº de Colisões] |
| 1.000.000       | 100.003        | ~10.0              | [Nº de Colisões]      | [Nº de Colisões]    | [Nº de Colisões] |
| ...             | ...            | ...                | ...                   | ...                 | ...        |


### 3.2. Gráficos Comparativos

**Como fazer:**
1.  Use os dados do `resultados.csv` no Excel, Google Sheets ou outra ferramenta para gerar gráficos.
2.  Salve os gráficos como imagens (ex: `grafico_tempo.png`).
3.  No site do GitHub, vá para o seu repositório, clique em "Add file" -> "Upload files" e envie as imagens. Crie uma pasta `imagens` para organizar.
4.  Insira as imagens no `README.md` usando a sintaxe abaixo.

**Gráfico 1: Tempo de Inserção vs. Fator de Carga**
![Tempo de Inserção](./imagens/grafico_tempo_insercao.png)
*Figura 1: Comparativo de tempo de inserção (em milissegundos) para as três estratégias com o aumento do fator de carga.*

**Gráfico 2: Número de Colisões vs. Fator de Carga**
![Número de Colisões](./imagens/grafico_colisoes.png)
*Figura 2: Comparativo do número total de colisões para cada estratégia.*

---

## 4. Análise e Discussão dos Resultados

**Esta é a parte mais importante do seu relatório.** Aqui você deve interpretar os dados das tabelas e gráficos. Responda a perguntas como:

* **Desempenho Geral:** Qual estratégia se mostrou mais eficiente em termos de tempo de inserção e busca? Houve diferença significativa entre elas?
* **Impacto do Fator de Carga:** Como o aumento do fator de carga (quando o número de registros é muito maior que o tamanho da tabela) afetou cada método? O desempenho do endereçamento aberto degradou mais rapidamente que o do encadeamento? Por quê?
* **Análise de Colisões:** Compare o número de colisões. O Hash Duplo realmente conseguiu uma distribuição melhor e menos colisões que a Sondagem Quadrática? O que os números mostram?
* **Análise de Gaps e Listas:** O que os dados de "maiores listas" (para encadeamento) e "gaps" (para endereçamento aberto) revelam sobre a qualidade da função de hash multiplicativa? Ela distribuiu bem as chaves? Um gap médio baixo ou uma distribuição uniforme das listas são bons indicadores.
* **Resultados Inesperados:** Houve algum cenário onde uma técnica teoricamente inferior se saiu melhor? Se sim, tente formular uma hipótese para explicar o porquê. Por exemplo, talvez a inserção ordenada no encadeamento tenha causado uma lentidão inesperada em cenários de alta colisão.

[**Escreva sua análise aqui.** Seja detalhado e sempre se baseie nos dados que você coletou.]

---

## 5. Conclusão

Com base nos resultados obtidos e na análise realizada, este trabalho conclui que [resuma sua principal descoberta]. Para aplicações que exigem [mencione um cenário, ex: alta performance sob alto fator de carga], a estratégia de [mencione a melhor estratégia, ex: Encadeamento Separado] se mostrou superior devido a [mencione o motivo, ex: sua resiliência à degradação de desempenho]. Por outro lado, para cenários com uso de memória mais crítico e fator de carga controlado, [mencione outra estratégia, ex: Hash Duplo] pode ser uma alternativa viável por [mencione a vantagem, ex: evitar o overhead dos ponteiros da lista].

O estudo prático reforçou os conceitos teóricos sobre tabelas hash, demonstrando o trade-off entre simplicidade de implementação, uso de memória e eficiência em diferentes condições de operação.

---

## 6. Como Executar o Projeto

1.  Clone este repositório.
2.  Certifique-se de ter o JDK [versão que você usou] ou superior instalado.
3.  Navegue até a pasta `src` e compile o arquivo Java:
    ```bash
    javac TabelaHash.java
    ```
4.  Execute o programa:
    ```bash
    java TabelaHash
    ```
5.  O programa irá primeiro gerar os arquivos de dados na pasta `/datasets` (se não existirem) e, em seguida, executará todos os testes.
6.  Ao final, o arquivo `resultados.csv` será gerado na raiz do projeto com todas as métricas coletadas.
