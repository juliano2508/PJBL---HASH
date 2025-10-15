import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class TabelaHash {

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

    public interface FuncaoHash {
        int hash(int chave, int tamanhoTabela);
    }

    public static class HashUtils {
        public static int hashMultiplicativo(int chave, int tamanho) {
            long A = 2654435761L; 
            long misturado = (chave & 0xFFFFFFFFL) * A;
            
            int r = Math.max(1, 32 - Integer.numberOfLeadingZeros(tamanho));
            int hash = (int) ((misturado >>> (32 - r)) & 0xFFFFFFFFL);
            
            return Math.floorMod(hash, tamanho);
        }

        public static int inverterDigitos(int x) {
            int rev = 0;
            x = Math.abs(x);
            for (int i = 0; i < 9; i++) {
                rev = rev * 10 + (x % 10);
                x /= 10;
            }
            return Math.abs(rev);
        }
    }

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
                b.cabeca = novo;
            } else {
                colisoes++; 
                
                No atual = b.cabeca;
                No anterior = null;
                
                while (atual != null && codigo > atual.registro.paraInteiro()) {
                    colisoes++;
                    anterior = atual;
                    atual = atual.proximo;
                }
                
                if (anterior == null) {
                    novo.proximo = b.cabeca;
                    b.cabeca = novo;
                } else {
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

        public Integer[] getTop3Listas() {
            Integer[] tamanhosListas = new Integer[tamanho];
            for (int i = 0; i < tamanho; i++) {
                tamanhosListas[i] = tabela[i].tamanho;
            }
            
            Arrays.sort(tamanhosListas, Comparator.reverseOrder());
            
            return new Integer[]{
                (tamanhosListas.length > 0 ? tamanhosListas[0] : 0),
                (tamanhosListas.length > 1 ? tamanhosListas[1] : 0),
                (tamanhosListas.length > 2 ? tamanhosListas[2] : 0)
            };
        }

        public double[] calcularGaps() {
            int anterior = -1, cont = 0;
            long soma = 0;
            int menor = Integer.MAX_VALUE, maior = Integer.MIN_VALUE;
            
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i].tamanho > 0) {
                    if (anterior != -1) {
                        int gap = i - anterior - 1;
                        soma += gap;
                        cont++;
                        if (gap < menor) menor = gap;
                        if (gap > maior) maior = gap;
                    }
                    anterior = i;
                }
            }
            
            if (cont == 0) return new double[]{0, 0, 0};
            return new double[]{menor, maior, ((double) soma) / cont};
        }
    }

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
                if (h2 == 0) h2 = 1;
                
                for (int i = 0; i < tamanho; i++) {
                    int pos = Math.floorMod(h1 + i * h2, tamanho);
                    if (!usado[pos]) {
                        tabela[pos] = r;
                        usado[pos] = true;
                        colisoes += i;
                        return true;
                    }
                }
                return false;
            } else {
                for (int i = 0; i < tamanho; i++) {
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
                    if (!usado[pos]) return false;
                    if (tabela[pos] != null && tabela[pos].equals(r)) return true;
                }
                return false;
            } else {
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
            
            for (int i = 0; i < tamanho; i++) {
                if (usado[i]) {
                    if (anterior != -1) {
                        int gap = i - anterior - 1;
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

    public static class GeradorDados {
        public static void gerar(String arquivo, long seed, long quantidade) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo))) {
                Random rnd = new Random(seed);
                for (long i = 0; i < quantidade; i++) {
                    int val = rnd.nextInt(1_000_000_000);
                    String codigo = String.format("%09d", val);
                    bw.write(codigo);
                    bw.newLine();
                }
            }
        }
    }

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
            String linhaFormatada = String.format(java.util.Locale.US,
                "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.4f\n",
                metodo, modo, tam, dados, tIns, tBusca,
                colisoes, l1, l2, l3, gMin, gMax, gMed);
            bw.write(linhaFormatada);
        }
        public void fechar() throws IOException { bw.close(); }
    }

    
    public static void main(String[] args) throws Exception {

        int[] tamanhos = {150001, 1500007, 15000017}; 
        
        long[] dados = {100_000L, 1_000_000L, 10_000_000L};
        long seed = 123456789L;

        String pasta = "datasets";
        String resultado = "resultados.csv";

        File dir = new File(pasta);
        if (!dir.exists()) dir.mkdirs();

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

        FuncaoHash hashMult = (k, M) -> HashUtils.hashMultiplicativo(k, M);
        FuncaoHash hashSec = (k, M) -> 1 + (Math.abs(k) % (M - 1)); 

        System.out.println("\n*** INICIANDO TESTES DE DESEMPENHO ***");
        System.out.println("Resultados detalhados serão salvos em: " + resultado);

        for (int M : tamanhos) {
            for (long qtd : dados) {
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
                
                System.out.println("-> Método 1: Encadeamento (Ordenado)");
                TabelaEncadeada enc = new TabelaEncadeada(M, hashMult);
                
                long t0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        enc.inserir(new Registro(linha.trim()));
                }
                long t1 = System.nanoTime();
                long tempoIns = t1 - t0;

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

                System.out.println("-> Método 2: Rehashing Duplo (Multiplicativo + Secundário)");
                TabelaEnderecAberto tdh = new TabelaEnderecAberto(M, hashMult, hashSec,
                        TabelaEnderecAberto.Modo.DUPLO);
                
                t0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        tdh.inserir(new Registro(linha.trim()));
                }
                t1 = System.nanoTime();
                tempoIns = t1 - t0;

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

                System.out.println("-> Método 3: Probing Quadrático (Multiplicativo)");
                TabelaEnderecAberto tq = new TabelaEnderecAberto(M, hashMult, null,
                        TabelaEnderecAberto.Modo.QUADRATICO);
                
                t0 = System.nanoTime();
                try (BufferedReader br = new BufferedReader(new FileReader(dataset))) {
                    String linha;
                    while ((linha = br.readLine()) != null)
                        tq.inserir(new Registro(linha.trim()));
                }
                t1 = System.nanoTime();
                tempoIns = t1 - t0;

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
