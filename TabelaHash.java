import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class TabelaHash {

    // Classe Registro
    public static class Registro {
        private final String codigo;

        public Registro(String codigo) {
            if (codigo == null) throw new IllegalArgumentException("Código nulo");
            if (codigo.length() != 9) throw new IllegalArgumentException("O código deve ter 9 dígitos: " + codigo);
            this.codigo = codigo;
        }

        public String getCodigo() { return codigo; }
        public int paraInteiro() { return Integer.parseInt(codigo); }
        @Override public String toString() { return codigo; }
        @Override public boolean equals(Object o) {
            return (o instanceof Registro) && this.codigo.equals(((Registro) o).codigo);
        }
        @Override public int hashCode() { return codigo.hashCode(); }
    }

    // Interface Função Hash
    public interface FuncaoHash {
        int hash(int chave, int tamanhoTabela);
    }

    // Classe utilitária com funções hash
    public static class HashUtils {
        // Hash multiplicativo (Knuth)
        public static int hashMultiplicativo(int chave, int tamanho) {
            // Constante A baseada no número áureo para hash multiplicativo
            long A = 2654435761L; 
            long misturado = (chave & 0xFFFFFFFFL) * A;
            
            // Acha o número de bits para o tamanho da tabela (log2(tamanho))
            // Garante que o resultado esteja dentro de [0, tamanho-1]
            int r = Math.max(1, 32 - Integer.numberOfLeadingZeros(tamanho));
            int hash = (int) ((misturado >>> (32 - r)) & 0xFFFFFFFFL);
            
            return Math.floorMod(hash, tamanho);
        }

        // Hash secundário: Inverte dígitos + Modulo (para Hash Duplo)
        public static int inverterDigitos(int x) {
            int rev = 0;
            x = Math.abs(x);
            // Inverte os dígitos da chave
            for (int i = 0; i < 9; i++) {
                rev = rev * 10 + (x % 10);
                x /= 10;
            }
            return Math.abs(rev);
        }
    }

    // Implementação com Encadeamento (Chaining)
    // - AGORA COM INSERÇÃO ORDENADA CONFORME REQUISITO
    public static class TabelaEncadeada {
        private final Balde[] tabela;
        private final int tamanho;
        private final FuncaoHash funcao;
        private long colisoes = 0;

        private static class No {
            Registro registro;
            No proximo;
            No(Registro r) { registro = r; }
        }

        private static class Balde {
            No cabeca;
            int tamanho;
        }

        public TabelaEncadeada(int tamanho, FuncaoHash funcao) {
            this.tamanho = tamanho;
            this.funcao = funcao;
            this.tabela = new Balde[tamanho];
            for (int i = 0; i < tamanho; i++) tabela[i] = new Balde();
        }

        public void inserir(Registro r) {
            int chave = r.paraInteiro();
            int codigo = r.paraInteiro();
            int indice = funcao.hash(chave, tamanho);
            Balde b = tabela[indice];
            
            No novo = new No(r);
            
            if (b.cabeca == null) {
                // Balde vazio
                b.cabeca = novo;
            } else {
                // CORREÇÃO: Contagem de colisões mais precisa.
                // Uma colisão ocorre quando o balde não está vazio. 
                // Colisões adicionais ocorrem para cada nó que precisamos percorrer na lista.
                colisoes++; 
                
                No atual = b.cabeca;
                No anterior = null;
                
                // 1. Busca a posição correta para inserção ORDENADA
                // Percorre enquanto o código for MENOR que o código atual na lista
                while (atual != null && codigo > atual.registro.paraInteiro()) {
                    colisoes++; // Colisão adicional por percorrer a lista
                    anterior = atual;
                    atual = atual.proximo;
                }
                
                // 2. Realiza a inserção
                if (anterior == null) {
                    // Inserção no início (novo nó é o menor)
                    novo.proximo = b.cabeca;
                    b.cabeca = novo;
                } else {
                    // Inserção no meio ou fim
                    novo.proximo = atual;
                    anterior.proximo = novo;
                }
            }
            b.tamanho++;
        }

        public boolean contem(Registro r) {
            int indice = funcao.hash(r.paraInteiro(), tamanho);
            No atual = tabela[indice].cabeca;
            while (atual != null) {
                if (atual.registro.equals(r)) return true;
                atual = atual.proximo;
            }
            return false;
        }

        public long getColisoes() { return colisoes; }

        // Retorna as 3 maiores listas
        public Integer[] getTop3Listas() {
            Integer[] tamanhosListas = new Integer[tamanho];
            for (int i = 0; i < tamanho; i++) {
                tamanhosListas[i] = tabela[i].tamanho;
            }
            
            // Ordena o array de tamanhos em ordem decrescente
            Arrays.sort(tamanhosListas, Comparator.reverseOrder());
            
            // CORREÇÃO: Torna a lógica mais segura para tabelas pequenas.
            // Retorna apenas os 3 primeiros (ou menos, se não houver 3)
            return new Integer[]{
                (tamanhosListas.length > 0 ? tamanhosListas[0] : 0),
                (tamanhosListas.length > 1 ? tamanhosListas[1] : 0),
                (tamanhosListas.length > 2 ? tamanhosListas[2] : 0)
            };
        }

        // Calcula Menor, Maior e Média de Gap
        public double[] calcularGaps() {
            int anterior = -1, cont = 0;
            long soma = 0;
            int menor = Integer.MAX_VALUE, maior = Integer.MIN_VALUE;
            
            // Percorre a tabela e mede o espaço entre baldes ocupados
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i].tamanho > 0) {
                    if (anterior != -1) {
                        int gap = i - anterior - 1; // gap = espaços livres entre o anterior e o atual
                        soma += gap;
                        cont++;
                        if (gap < menor) menor = gap;
                        if (gap > maior) maior = gap;
                    }
                    anterior = i; // Atualiza a posição do último balde ocupado
                }
            }
            
            if (cont == 0) return new double[]{0, 0, 0};
            return new double[]{menor, maior, ((double) soma) / cont};
        }
    }

    // Implementação com Endereçamento Aberto (Rehashing)
    public static class TabelaEnderecAberto {
        private final Registro[] tabela;
        private final boolean[] usado;
        private final int tamanho;
        private long colisoes = 0;
        private final FuncaoHash f1;
        private final FuncaoHash f2;
        private final Modo modo;

        public enum Modo { DUPLO, QUADRATICO }

        public TabelaEnderecAberto(int tamanho, FuncaoHash f1, FuncaoHash f2, Modo modo) {
            this.tamanho = tamanho;
            this.f1 = f1;
            this.f2 = f2;
            this.modo = modo;
            this.tabela = new Registro[tamanho];
            this.usado = new boolean[tamanho];
        }

        public boolean inserir(Registro r) {
            int chave = r.paraInteiro();
            int h1 = f1.hash(chave, tamanho);
            
            if (modo == Modo.DUPLO) {
                int h2 = f2.hash(HashUtils.inverterDigitos(chave), tamanho);
                if (h2 == 0) h2 = 1; // Garante que o passo não seja zero
                
                for (int i = 0; i < tamanho; i++) {
                    int pos = Math.floorMod(h1 + i * h2, tamanho);
                    if (!usado[pos]) {
                        tabela[pos] = r;
                        usado[pos] = true;
                        colisoes += i; // A colisão é contada a partir da primeira tentativa (i=0)
                        return true;
                    }
                }
                return false; // Tabela cheia ou loop
            } else { // Probing Quadrático
                for (int i = 0; i < tamanho; i++) {
                    // Função de probing quadrático: (h1 + c1*i + c2*i^2) mod M. Usando c1=1 e c2=3.
                    // O valor i representa o número de colisões antes da inserção
                    int pos = Math.floorMod(h1 + i + 3 * i * i, tamanho); 
                    if (!usado[pos]) {
                        tabela[pos] = r;
                        usado[pos] = true;
                        colisoes += i;
                        return true;
                    }
                }
                return false;
            }
        }

        public boolean contem(Registro r) {
            int chave = r.paraInteiro();
            int h1 = f1.hash(chave, tamanho);
            
            if (modo == Modo.DUPLO) {
                int h2 = f2.hash(HashUtils.inverterDigitos(chave), tamanho);
                if (h2 == 0) h2 = 1;
                
                for (int i = 0; i < tamanho; i++) {
                    int pos = Math.floorMod(h1 + i * h2, tamanho);
                    if (!usado[pos]) return false; // Parada da busca: posição nunca foi usada
                    if (tabela[pos] != null && tabela[pos].equals(r)) return true;
                }
                return false;
            } else { // Probing Quadrático
                for (int i = 0; i < tamanho; i++) {
                    int pos = Math.floorMod(h1 + i + 3 * i * i, tamanho);
                    if (!usado[pos]) return false;
                    if (tabela[pos] != null && tabela[pos].equals(r)) return true;
                }
                return false;
            }
        }

        public long getColisoes() { return colisoes; }

        public double[] calcularGaps() {
            int anterior = -1, cont = 0;
            long soma = 0;
            int menor = Integer.MAX_VALUE, maior = Integer.MIN_VALUE;
            
            // Percorre o vetor e mede o espaço entre elementos usados
            for (int i = 0; i < tamanho; i++) {
                if (usado[i]) {
                    if (anterior != -1) {
                        int gap = i - anterior - 1; // gap = espaços livres entre o anterior e o atual
                        soma += gap;
                        cont++;
                        if (gap < menor) menor = gap;
                        if (gap > maior) maior = gap;
                    }
                    anterior = i;
                }
            }
            
            if (cont <= 0) return new double[]{0, 0, 0};
            return new double[]{menor, maior, ((double) soma) / cont};
        }
    }

    // Geração de dados com seed fixa (Gera arquivo para leitura)
    public static class GeradorDados {
        public static void gerar(String arquivo, long seed, long quantidade) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo))) {
                Random rnd = new Random(seed);
                for (long i = 0; i < quantidade; i++) {
                    int val = rnd.nextInt(1_000_000_000); // 9 dígitos
                    String codigo = String.format("%09d", val);
                    bw.write(codigo);
                    bw.newLine();
                }
            }
        }
    }

    // Escrita dos resultados em CSV (Permite a geração de gráficos)
    public static class EscritorCSV {
        private final BufferedWriter bw;
        public EscritorCSV(String arquivo) throws IOException {
            bw = new BufferedWriter(new FileWriter(arquivo));
        }
        public void cabecalho() throws IOException {
            bw.write("metodo,modo,tamanhoTabela,tamanhoDataset,tempoInsercaoNs,tempoBuscaNs,colisoes,lista1,lista2,lista3,gapMin,gapMax,gapMedio\n");
        }
        public void linha(String metodo, String modo, int tam, long dados,
                              long tIns, long tBusca, long colisoes,
                              Integer l1, Integer l2, Integer l3, double gMin, double gMax, double gMed) throws IOException {
            // CORREÇÃO: Usando Locale.US para garantir que o separador decimal seja '.' e não ','
            String linhaFormatada = String.format(java.util.Locale.US,
                "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.4f\n",
                metodo, modo, tam, dados, tIns, tBusca,
                colisoes, l1, l2, l3, gMin, gMax, gMed);
            bw.write(linhaFormatada);
        }
        public void fechar() throws IOException { bw.close(); }
    }

    // Execução principal
    public static void main(String[] args) throws Exception {

        // CORREÇÃO: Tamanhos da tabela ajustados para serem MAIORES que os datasets,
        // o que é essencial para o funcionamento do Endereçamento Aberto.
        // Foram escolhidos números primos para melhorar a distribuição.
        int[] tamanhos = {150001, 1500007, 15000017}; 
        
        // Tamanhos dos conjuntos de dados
        long[] dados = {100_000L, 1_000_000L, 10_000_000L};
        long seed = 123456789L;

        String pasta = "datasets";
        String resultado = "resultados.csv";

        File dir = new File(pasta);
        if (!dir.exists()) dir.mkdirs();

        // Gera os arquivos com seed fixa
        for (long qtd : dados) {
            String arq = pasta + "/dados_" + qtd + ".txt";
            if (!new File(arq).exists()) {
                System.out.println("Gerando " + arq + " (código: 9 dígitos)...");
                GeradorDados.gerar(arq, seed, qtd);
            } else {
                System.out.println("Arquivo já existe: " + arq);
            }
        }

        EscritorCSV csv = new EscritorCSV(resultado);
        csv.cabecalho();

        // Funções Hash escolhidas:
        FuncaoHash hashMult = (k, M) -> HashUtils.hashMultiplicativo(k, M);
        // CORREÇÃO: Usando Math.abs(k) para garantir que o resultado do módulo seja sempre positivo.
        FuncaoHash hashSec = (k, M) -> 1 + (Math.abs(k) % (M - 1)); // Hash secundário para Hash Duplo

        System.out.println("\n*** INICIANDO TESTES DE DESEMPENHO ***");
        System.out.println("Resultados detalhados serão salvos em: " + resultado);

        for (int M : tamanhos) {
            for (long qtd : dados) {
                // CORREÇÃO: Pula combinações onde a quantidade de dados é maior que o tamanho da tabela,
                // pois isso faria o Encadeamento ficar muito lento e quebraria o Endereçamento Aberto.
                if (qtd > M) {
                    System.out.println("\n=======================================================");
                    System.out.println("AVISO: Pulando teste para Tabela=" + M + " | Registros=" + qtd + " (dataset maior que a tabela)");
                    System.out.println("=======================================================");
                    continue;
                }

                String dataset = pasta + "/dados_" + qtd + ".txt";
                System.out.println("\n=======================================================");
                System.out.println("Teste | Tabela=" + M + " | Registros=" + qtd);
                System.out.println("=======================================================");
                
                // --- 1. Encadeamento (Inserção Ordenada) ---
                System.out.println("-> Método 1: Encadeamento (Ordenado)");
                TabelaEncadeada enc = new TabelaEncadeada(M, hashMult);
                
                // Inserção
                long t0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        enc.inserir(new Registro(linha.trim()));
                }
                long t1 = System.nanoTime();
                long tempoIns = t1 - t0;

                // Busca
                long b0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        enc.contem(new Registro(linha.trim()));
                }
                long b1 = System.nanoTime();
                long tempoBusca = b1 - b0;

                double[] gaps = enc.calcularGaps();
                Integer[] topListas = enc.getTop3Listas();
                
                csv.linha("encadeamento", "ordenado_multiplicativo", M, qtd, tempoIns, tempoBusca,
                            enc.getColisoes(), topListas[0], topListas[1], topListas[2], gaps[0], gaps[1], gaps[2]);

                // --- 2. Rehashing Duplo ---
                System.out.println("-> Método 2: Rehashing Duplo (Multiplicativo + Secundário)");
                TabelaEnderecAberto tdh = new TabelaEnderecAberto(M, hashMult, hashSec,
                        TabelaEnderecAberto.Modo.DUPLO);
                
                // Inserção
                t0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        tdh.inserir(new Registro(linha.trim()));
                }
                t1 = System.nanoTime();
                tempoIns = t1 - t0;

                // Busca
                b0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        tdh.contem(new Registro(linha.trim()));
                }
                b1 = System.nanoTime();
                tempoBusca = b1 - b0;

                double[] gapsDH = tdh.calcularGaps();
                csv.linha("enderecamento_aberto", "duplo", M, qtd, tempoIns, tempoBusca,
                            tdh.getColisoes(), 0, 0, 0, gapsDH[0], gapsDH[1], gapsDH[2]);

                // --- 3. Probing Quadrático ---
                System.out.println("-> Método 3: Probing Quadrático (Multiplicativo)");
                TabelaEnderecAberto tq = new TabelaEnderecAberto(M, hashMult, null,
                        TabelaEnderecAberto.Modo.QUADRATICO);
                
                // Inserção
                t0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        tq.inserir(new Registro(linha.trim()));
                }
                t1 = System.nanoTime();
                tempoIns = t1 - t0;

                // Busca
                b0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        tq.contem(new Registro(linha.trim()));
                }
                b1 = System.nanoTime();
                tempoBusca = b1 - b0;

                double[] gapsQ = tq.calcularGaps();
                csv.linha("enderecamento_aberto", "quadratico", M, qtd, tempoIns, tempoBusca,
                            tq.getColisoes(), 0, 0, 0, gapsQ[0], gapsQ[1], gapsQ[2]);
            }
        }

        csv.fechar();
        System.out.println("\n*** EXPERIMENTOS CONCLUÍDOS COM SUCESSO ***");
        System.out.println("O arquivo 'resultados.csv' contém todas as métricas necessárias para o relatório, gráficos e tabelas.");
    }
}