/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServidorChatUDP;

import UDP.*;
import java.awt.ComponentOrientation;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import javafx.scene.control.TableSelectionModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author felipesoares
 */
public class ClientChatUDP extends javax.swing.JFrame {

    
    private JFrame frame;
    private Socket socket = null;
    private boolean conectado = false;
    private int x = 0;
    public static void main(String[] argv){new ClientChatUDP();}
    private String[] contatosList = new String[1];
    private DatagramPacket packet = null;
    private DatagramSocket clientSocket = null;
    private final ClientChatUDP clientChatUDP;
    private ArrayList<String[]> contatosConectados = new ArrayList<>();
    private Thread rxMensagensThread;
    private DefaultTableModel table;
    private boolean conexaoAceita = false;
    
    /**
     * Creates new form Client
     */
    public ClientChatUDP() {
        initComponents();
        clientChatUDP = this;
        
        ipTextField.setText("localhost");
        portaTextField.setText("20000");
        mensagemTextField.setText("");
        conectarBtn.setText("Conectar");
        respostaTextArea.setLineWrap(true);
        table = iniciaJTable(contatosJTable);
        
        this.CriaJanela();
        
        mensagemBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                mensagemActionPerformed(e);
            }
            
        });
        
        conectarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                conectarActionPerformed(e);
            }
        });
        
    }
    
    private void conectarActionPerformed(ActionEvent e) {
        if(!conectado) {
            if(solicitarConexao()){
                ipTextField.setEnabled(false);
                portaTextField.setEnabled(false);
                nomeusrjTextField.setEnabled(false);
                conectarBtn.setText("Desconectar");
                conectado = true;
            }
        } else {
            if(solicitarDesconexao()){
                ipTextField.setEnabled(true);
                portaTextField.setEnabled(true);
                nomeusrjTextField.setEnabled(true);
                conectarBtn.setText("Conectar");
                conectado = false;
            }
        }
        
        
    }
    
    private boolean enviarMensagem(String mensagemStr){
        try{                       
            respostaTextArea.setText(respostaTextArea.getText()+"\n"+mensagemStr);
            mensagemStr = mensagemStr.trim();
            byte[] messageByte = mensagemStr.getBytes();
            
            packet = new DatagramPacket(messageByte, messageByte.length, InetAddress.getByName(ipTextField.getText()), Integer.parseInt(portaTextField.getText()));
            System.out.println(mensagemStr);
            clientSocket.send(packet);
            respostaTextArea.setText(respostaTextArea.getText()+"\nMensagem enviada com sucesso!\n\n");
            return true;
        } catch(Exception e) { return false;}
    }
    
    private boolean abrirRecepcaoMensagens() {
        try{
            rxMensagensThread = new Thread(() -> {
                try{
                    String[] datagrama;
                    DatagramPacket mensagemPkt = new DatagramPacket(new byte[10000], 10000,
                                InetAddress.getByName(ipTextField.getText()), Integer.parseInt(portaTextField.getText()));
                    while(true){
                        mensagemPkt.setData(new byte[10000]);
                        clientSocket.receive(mensagemPkt);
                        String receiveStr = new String(mensagemPkt.getData());
                        receiveStr = receiveStr.trim();
                        datagrama = receiveStr.split("#");
                        System.out.println(receiveStr);
                        System.out.println(datagrama[3]);
                        switch(datagrama[0].charAt(0)){
                            case '2':
                                conexaoAceita = true;
                                atualizaListaContatos(datagrama);
                                break;
                            
                            case '4':
                                recepcaoMensagem(datagrama);
                                break;
                            default:
                                respostaTextArea.setText(respostaTextArea.getText()+" "+"\nMensagem inválida recebida!");
                                // enviar mensagem avisando o erro pra quem mandou
                        }
                        
                        datagrama = null;
                        
                        
                        Thread.sleep(500);
                    }
                } catch (Exception e){
                    //Mensagem ao usuário?!
                }
            });
            rxMensagensThread.start();
            
            return true;
        } catch(Exception e){
            return false;
        }
    }
    
    private boolean solicitarDesconexao() {
        if(enviarMensagem("5#")){
            rxMensagensThread.interrupt();
            atualizaListaContatos(new String[]{});
            return true;
        }
        return false;
    }
    
    private boolean solicitarConexao() {
        try{
            clientSocket = new DatagramSocket();
            if(enviarMensagem("1#"+nomeusrjTextField.getText())){
                if(abrirRecepcaoMensagens()){
                    return true;
                }
            }
            
        } catch (Exception e){}
        rxMensagensThread.interrupt();
        return false;
    }

    private void mensagemActionPerformed(ActionEvent ae) {
        int idxSelecionado = contatosJTable.getSelectedRow();
        String mensagem = null;
        String ip = null;
        String porta = null;
        
        if(broadcastjCheckBox.isSelected()) {
            ip = "999.999.999.999";
            porta = "99999";
        }
        else if(idxSelecionado < 0){
            return;
        }
        else {
            ip = String.valueOf(contatosJTable.getValueAt(idxSelecionado, 1));
            porta = String.valueOf(contatosJTable.getValueAt(idxSelecionado, 2));
        }
        mensagem = mensagemTextField.getText();

        enviarMensagem("3#"+ip+"#"+porta+"#"+mensagem);
        mensagemTextField.setText("");
    }
    
    
    private void atualizaListaContatos(String[] contatos){
        contatosConectados.clear();
        table.getDataVector().removeAllElements();
        String [] dados;
        for(int i = 1; i < contatos.length; i+=3 ) {
            dados = new String[]{contatos[i+2], contatos[i], contatos[i+1]};
            contatosConectados.add(dados);
            table.addRow(dados);
        }
    }
    
    private void recepcaoMensagem(String[] pacote){
        respostaTextArea.setText(respostaTextArea.getText()+"\n"+pacote[1]+":"+pacote[2]+" diz:\n"+pacote[3]);
    }
    
    
    
    
    
    
    
    // MÉTODOS PARA CRIAÇÃO E CONFIGURAÇÃO DA JANELA

    private void CriaJanela() {
        frame = new JFrame("Mensagem");
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 300);
        frame.setLocation(frame.getToolkit().getScreenSize().width/2 - frame.getWidth()+frame.getToolkit().getScreenSize().width, frame.getToolkit().getScreenSize().height/2 - frame.getHeight()/2);
        frame.setVisible(true);
    }

    private DefaultTableModel iniciaJTable(JTable table) {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contatosJTable.setSelectionModel(selectionModel);

        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        
        tableModel.setColumnIdentifiers(new Object[]{"Nome", "IP", "Porta"});
        tableModel.setNumRows(0);
        
        return tableModel;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDesktopPane1 = new javax.swing.JDesktopPane();
        jPanel = new javax.swing.JPanel();
        mensagensJPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        respostaTextArea = new javax.swing.JTextArea();
        mensagemBtn = new javax.swing.JButton();
        mensagemTextField = new javax.swing.JTextField();
        conexaoJPanel = new javax.swing.JPanel();
        portaTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        ipTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        conectarBtn = new javax.swing.JButton();
        nomeusrjTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        clientesJPanel = new javax.swing.JPanel();
        broadcastjCheckBox = new javax.swing.JCheckBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        contatosJTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        mensagensJPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        respostaTextArea.setColumns(20);
        respostaTextArea.setRows(5);
        respostaTextArea.setEnabled(false);
        jScrollPane1.setViewportView(respostaTextArea);

        mensagemBtn.setText("Enviar");

        javax.swing.GroupLayout mensagensJPanelLayout = new javax.swing.GroupLayout(mensagensJPanel);
        mensagensJPanel.setLayout(mensagensJPanelLayout);
        mensagensJPanelLayout.setHorizontalGroup(
            mensagensJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mensagensJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mensagensJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mensagensJPanelLayout.createSequentialGroup()
                        .addComponent(mensagemTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mensagemBtn)))
                .addContainerGap())
        );
        mensagensJPanelLayout.setVerticalGroup(
            mensagensJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mensagensJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mensagensJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mensagemBtn)
                    .addComponent(mensagemTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        conexaoJPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel2.setText("Porta");

        jLabel1.setText("IP");

        conectarBtn.setText("jButton1");

        jLabel3.setText("Nome de usuário");

        javax.swing.GroupLayout conexaoJPanelLayout = new javax.swing.GroupLayout(conexaoJPanel);
        conexaoJPanel.setLayout(conexaoJPanelLayout);
        conexaoJPanelLayout.setHorizontalGroup(
            conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(conexaoJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nomeusrjTextField)
                    .addGroup(conexaoJPanelLayout.createSequentialGroup()
                        .addGroup(conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ipTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(conexaoJPanelLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(conexaoJPanelLayout.createSequentialGroup()
                                .addComponent(portaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(conectarBtn)))))
                .addContainerGap())
        );
        conexaoJPanelLayout.setVerticalGroup(
            conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, conexaoJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nomeusrjTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(conexaoJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ipTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(conectarBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        clientesJPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        broadcastjCheckBox.setText("Broadcast");

        contatosJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        contatosJTable.setShowGrid(false);
        contatosJTable.setShowHorizontalLines(true);
        jScrollPane3.setViewportView(contatosJTable);
        if (contatosJTable.getColumnModel().getColumnCount() > 0) {
            contatosJTable.getColumnModel().getColumn(2).setResizable(false);
        }

        javax.swing.GroupLayout clientesJPanelLayout = new javax.swing.GroupLayout(clientesJPanel);
        clientesJPanel.setLayout(clientesJPanelLayout);
        clientesJPanelLayout.setHorizontalGroup(
            clientesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(clientesJPanelLayout.createSequentialGroup()
                .addComponent(broadcastjCheckBox)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        clientesJPanelLayout.setVerticalGroup(
            clientesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(clientesJPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(broadcastjCheckBox)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelLayout = new javax.swing.GroupLayout(jPanel);
        jPanel.setLayout(jPanelLayout);
        jPanelLayout.setHorizontalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(conexaoJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(clientesJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mensagensJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelLayout.setVerticalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mensagensJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelLayout.createSequentialGroup()
                        .addComponent(conexaoJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clientesJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox broadcastjCheckBox;
    private javax.swing.JPanel clientesJPanel;
    private javax.swing.JButton conectarBtn;
    private javax.swing.JPanel conexaoJPanel;
    private javax.swing.JTable contatosJTable;
    private javax.swing.JTextField ipTextField;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton mensagemBtn;
    private javax.swing.JTextField mensagemTextField;
    private javax.swing.JPanel mensagensJPanel;
    private javax.swing.JTextField nomeusrjTextField;
    private javax.swing.JTextField portaTextField;
    private javax.swing.JTextArea respostaTextArea;
    // End of variables declaration//GEN-END:variables
}
