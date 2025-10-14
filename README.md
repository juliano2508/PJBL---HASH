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
* **Versão do JDK:** 21
* **Sistema Operacional:** Windows 11
* **IDE:** Visual Studio Code

### 2.2. Geração de Dados
Os conjuntos de dados foram gerados de forma programática para garantir a reprodutibilidade dos testes.
* **Tamanhos dos Datasets:** 100.000, 1.000.000 e 10.000.000 de registros.
* **Formato do Registro:** Código de 9 dígitos (`String` formatada como "000000000" a "999999999").
* **Seed Aleatória:** Foi utilizada uma `seed` fixa (`123456789L`) para o gerador de números aleatórios, garantindo que os mesmos conjuntos de dados fossem usados em todos os testes.

### 2.3. Tamanhos da Tabela Hash
Foram escolhidos três tamanhos para o vetor da tabela, com variação de aproximadamente 10x entre eles. Optou-se por números primos para melhorar a distribuição das chaves e minimizar colisões.
* **Tamanho 1:** 1.009
* **Tamanho 2:** 10.007
* **Tamanho 3:** 100.003

### 2.4. Estratégias Implementadas

Foram implementadas três estratégias distintas para o tratamento de colisões.

#### Estratégia 1: Encadeamento Separado (com Inserção Ordenada)
Nesta abordagem, cada posição da tabela hash aponta para uma lista ligada ("balde"). Quando ocorre uma colisão, o novo registro é adicionado à lista correspondente.
* **Função Hash Principal:** Foi utilizado o **Método da Multiplicação** (sugerido por Knuth), que calcula o índice como `hash = floor(M * (k * A mod 1))`. Esta função é conhecida por sua boa distribuição, independentemente da escolha do tamanho `M`.
* **Detalhe de Implementação:** Os elementos foram inseridos de forma **ordenada** dentro de cada lista ligada. Essa escolha pode aumentar ligeiramente o custo de inserção em listas longas, mas garante uma estrutura de dados previsível.

#### Estratégia 2: Endereçamento Aberto (Sondagem Quadrática)
A sondagem quadrática é uma estratégia de endereçamento aberto que busca resolver colisões tentando posições progressivamente mais distantes da original, seguindo uma função quadrática, o que ajuda a mitigar o problema de agrupamento primário.
* **Função de Sondagem:** `h(k, i) = (h1(k) + c1*i + c2*i²) mod M`. No código, foram usados os coeficientes `c1=1` e `c2=3`.
* **Função Hash Principal (h1):** Também utiliza o Método da Multiplicação.

#### Estratégia 3: Endereçamento Aberto (Hash Duplo)
O Hash Duplo é considerado uma das técnicas mais robustas de endereçamento aberto, pois utiliza uma segunda função hash para calcular o "passo" da sondagem. Isso gera uma sequência de sondagem única para cada chave, eliminando os problemas de agrupamento primário e secundário.
* **Função Hash Principal (h1):** Método da Multiplicação.
* **Função Hash Secundária (h2):** A função `h2` foi implementada como `h2(k) = 1 + (inverter(k) % (M-1))`, garantindo que o passo da sondagem nunca seja zero.

---

## 3. Resultados

Os testes foram executados e os resultados foram compilados. As tabelas abaixo apresentam um resumo dos dados coletados para os cenários mais representativos.

### 3.1. Tabelas de Desempenho

**Tabela 1: Tempo de Inserção (em milissegundos)**

| Tamanho Dataset | Tamanho Tabela | Fator de Carga (α) | Encadeamento Ordenado | Sondagem Quadrática | Hash Duplo |
|:---------------:|:--------------:|:------------------:|:---------------------:|:-------------------:|:----------:|
| 100.000         | 100.003        | ~1.0               | 28 ms                 | 22 ms               | **19 ms** |
| 1.000.000       | 100.003        | ~10.0              | **950 ms** | > 5 min* | > 5 min* |
| 10.000.000      | 100.003        | ~100.0             | **12.300 ms** | N/A* | N/A* |

*\*Nota: Nos cenários onde o Fator de Carga (α) é maior que 1, as tabelas de Endereçamento Aberto não conseguem inserir todos os elementos, pois o vetor fica cheio. O tempo de execução torna-se extremamente alto devido às tentativas de inserção falhas.*

**Tabela 2: Número de Colisões**

| Tamanho Dataset | Tamanho Tabela | Fator de Carga (α) | Encadeamento Ordenado | Sondagem Quadrática | Hash Duplo       |
|:---------------:|:--------------:|:------------------:|:---------------------:|:-------------------:|:-----------------|
| 100.000         | 100.003        | ~1.0               | 39.102                | 265.418             | **211.850** |
| 1.000.000       | 100.003        | ~10.0              | **899.997** | > 450.000.000       | > 400.000.000    |

*Nota: Para o Encadeamento, uma "colisão" significa que um balde já continha um elemento. Para o Endereçamento Aberto, as colisões representam o número total de "saltos" (probes) necessários para encontrar um espaço vazio.*

---

## 4. Análise e Discussão dos Resultados

A análise dos dados coletados revela diferenças críticas de desempenho entre as estratégias, principalmente relacionadas ao **fator de carga (`α`)**.

* **Desempenho Geral em Baixo Fator de Carga (α < 1):**
    Quando a tabela é grande o suficiente para acomodar todos os elementos, as estratégias de **Endereçamento Aberto (Hash Duplo e Sondagem Quadrática) são superiores em velocidade de inserção**. Isso ocorre devido à melhor localidade de cache (acessando diretamente posições de um vetor) e à ausência do custo de alocação de memória para novos nós (`Node`), que é inerente ao Encadeamento. O Hash Duplo se mostrou ligeiramente mais eficiente que a Sondagem Quadrática, registrando um número menor de colisões, o que confirma sua capacidade de espalhar melhor os elementos e evitar agrupamentos.

* **Impacto Crítico do Fator de Carga (α > 1):**
    Este é o ponto mais importante da análise. As estratégias de **Endereçamento Aberto falham catastroficamente quando o número de elementos excede o tamanho da tabela**. Uma vez que o vetor está cheio, qualquer nova tentativa de inserção resulta em uma busca exaustiva e infrutífera por um espaço vazio, levando o tempo de execução a níveis impraticáveis e consumindo 100% da CPU. O número de colisões explode, pois cada uma das centenas de milhares de inserções falhas percorre a tabela inteira.

    Em contraste, o **Encadeamento Separado demonstra uma degradação de desempenho graciosa**. Mesmo com um fator de carga de 100 (uma média de 100 elementos por lista), a estrutura continua funcional. O tempo de inserção aumenta de forma previsível, pois as listas encadeadas se tornam mais longas, mas o sistema permanece operacional e capaz de inserir todos os registros.

* **Análise de Colisões:**
    Os números confirmam a teoria: o Hash Duplo gera uma sequência de sondagem mais eficiente, resultando em menos colisões totais que a Sondagem Quadrática em cenários funcionais (α < 1). No entanto, a métrica de colisões para o Encadeamento tem um significado diferente e mais benigno: ela simplesmente indica quantos elementos foram para listas não vazias, o que é uma ocorrência normal e esperada.

* **Análise de Gaps e Listas:**
    A análise das maiores listas no Encadeamento e dos gaps no Endereçamento Aberto (não mostrados nas tabelas, mas observados durante os testes) indicou que a **função de hash multiplicativa fez um bom trabalho na distribuição das chaves**. Não foram observadas listas excessivamente longas ou grandes "desertos" (gaps) na tabela, sugerindo uma distribuição de chaves uniforme e eficaz.

---

## 5. Conclusão

Com base nos resultados obtidos, concluímos que a escolha da estratégia de tratamento de colisão depende fundamentalmente do conhecimento prévio sobre o volume de dados e o fator de carga da aplicação.

A estratégia de **Encadeamento Separado é a mais robusta e versátil**. Sua capacidade de operar de forma eficiente mesmo com fatores de carga muito superiores a 1 a torna a escolha ideal para sistemas de propósito geral, onde o número de elementos pode flutuar ou é difícil de prever. Ela oferece previsibilidade e estabilidade em detrimento de um pequeno overhead de memória (para os ponteiros) e um desempenho ligeiramente inferior em condições ideais.

Por outro lado, o **Endereçamento Aberto, especialmente com Hash Duplo, é a alternativa de maior performance para cenários especializados**. Em aplicações onde o uso de memória é crítico e o número máximo de elementos é conhecido e controlado (garantindo um `α` baixo, idealmente < 0.7), ele oferece velocidades de inserção e busca superiores devido à sua simplicidade estrutural e vantagens de cache. Contudo, seu uso é desaconselhado em ambientes com volume de dados imprevisível devido ao seu risco de falha catastrófica.

Este trabalho prático reforçou a importância de entender os trade-offs teóricos entre as estruturas de dados, demonstrando que não existe uma "melhor" solução universal, mas sim a mais adequada para cada contexto de problema.

---

## 6. Como Executar o Projeto

1.  Clone este repositório.
2.  Certifique-se de ter o JDK 21 ou superior instalado e configurado nas variáveis de ambiente do sistema.
3.  Abra um terminal na pasta raiz do projeto.
4.  Compile o arquivo Java:
    ```bash
    javac TabelaHash.java
    ```
5.  Execute o programa:
    ```bash
    java TabelaHash
    ```
6.  O programa irá primeiro gerar os arquivos de dados na pasta `datasets/` (se não existirem) e, em seguida, executará todos os testes.
7.  Ao final, o arquivo `resultados.csv` será gerado na raiz do projeto com todas as métricas detalhadas coletadas.
