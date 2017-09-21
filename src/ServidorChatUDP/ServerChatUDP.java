/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServidorChatUDP;

import UDP.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author felipesoares
 */
public class ServerChatUDP extends javax.swing.JPanel {

    private JFrame frame;
    private DefaultTableModel table;
    private ArrayList<String[]> clientesConectados = new ArrayList<>();
    private DatagramPacket receivePkt;
    private byte[] buffer = null;
    private Thread execServidor;
    private DatagramSocket serverDatagram = null;

                        
    private ServerChatUDP() {
        initComponents();

        table = iniciaJTable(clientesJTable);
        

        this.CriaJanela();
        
        try {

            serverTextArea.setText("Server starded on port 20000\n");
            
            serverDatagram = new DatagramSocket(20000);
            
            execServidor = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Entrou na Thread");

                    try {
                       
                        while(true)
                        {
                            buffer = new byte[1024];
                            receivePkt = new DatagramPacket(buffer, buffer.length);
                            serverDatagram.receive(receivePkt);

                            String receiveStr = new String(receivePkt.getData());

                            receiveStr = receiveStr.trim();
                            

                            serverTextArea.setText(serverTextArea.getText()+"\n"
                                    +receivePkt.getAddress().toString().split("/")[1]+":"
                                    +receivePkt.getPort()+"\n"+receiveStr);

                            String[] datagrama = receiveStr.split("#");
                            String ip = receivePkt.getAddress().toString().split("/")[1];
                            // Tratar o tipo de mensagem
                            switch (datagrama[0].charAt(0))
                            {
                                case '1':
                                    addConexao(datagrama[1], ip, receivePkt.getPort());
                                    break;
                                case '3':
                                    encaminharMensagem(datagrama, ip, receivePkt.getPort());
                                    break;
                                case '5':
                                    removeConexao(ip, receivePkt.getPort());
                                    break;
                                default:
                                    serverTextArea.setText(serverTextArea.getText()+" "+receivePkt.getPort()+"\nMensagem inválida");
                                    // enviar mensagem avisando o erro pra quem mandou

                            }
                            Thread.sleep(1000);

                        }
                                                
                    } catch (Exception e) {
                        System.out.println(e);
                    } finally {
                        if(serverDatagram != null)
                            serverDatagram.close();
                    }


                }
            });
            
            execServidor.start();
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    // PROCESSAMENTO DOS DATAGRAMAS RECEBIDOS
    
    private void removeConexao(String ip, int porta){
        int idx = encontraIndice(clientesConectados, ip, porta);
        JOptionPane.showMessageDialog(frame, idx);
        if(idx > -1){
            table.removeRow(idx);
            clientesConectados.remove(idx);   

            enviaListaConectados("999.999.999.999", 99999);
            //mandar datagrama 2 para todos os conectados

        }
    }
    
    private void addConexao(String nome, String ip, int porta ){
        table.addRow(new Object[]{nome, ip, porta});
        clientesConectados.add(new String[]{nome,ip,String.valueOf(porta)});
        
        enviaListaConectados("999.999.999.999", 99999);
        //mandar datagrama 2 para todos os conectados
    }
    
    private void encaminharMensagem(String[] mensagem, String ip, int porta) {
        
        enviarMensagem("4#"+ip+"#"+porta+"#"+mensagem[3], mensagem[1], Integer.parseInt(mensagem[2]));

        
    }
    private boolean isBroadcast(String ip, String porta){
        if(ip.equals("999.999.999.999") && porta.equals("99999"))
            return true;
        return false;
    }
    
    private boolean enviaListaConectados(String ip, int porta){
        String mensagem ="";
        
        mensagem += clientesConectados.stream().map((str) -> "#"+str[1]+"#"+str[2]+"#"+str[0]).reduce(mensagem+"2", String::concat);
        
        if(!enviarMensagem(mensagem, ip, porta))
            return false;
        
        
        return true;
    }
    
    private boolean enviarMensagem(String mensagemStr, String ip, int porta){

        DatagramPacket enviar = null;
        try {
            if(isBroadcast(ip, String.valueOf(porta))){
                for(String[] str : clientesConectados){
                    System.out.println(mensagemStr+"-"+str[1]+"-"+str[2]);
                    enviar = new DatagramPacket(mensagemStr.getBytes(), mensagemStr.getBytes().length,
                                InetAddress.getByName(str[1]), Integer.parseInt(str[2]));
                    serverDatagram.send(enviar);
                }
            } else {
                enviar = new DatagramPacket(mensagemStr.getBytes(), mensagemStr.getBytes().length,
                            InetAddress.getByName(ip), porta);
                serverDatagram.send(enviar);
            }
            

         } catch (Exception e) {
            System.out.println("Deu pau no enviarMensagem!!!!\n"+e);
            
            return false;
        }
        return true;
    }
    
    private int encontraIndice(ArrayList<String[]> string, String ip, int porta) {
        int idx = 0;
        for(String[] str : string){
            
            if(str[1].equals(ip) && str[2].equals(String.valueOf(porta)))
              return idx;
            idx++;
        }
        
        return -1;
    }
    
    
    // MÉTODOS PARA CRIAÇÃO E CONFIGURAÇÃO DA JANELA
    public static void main(String[] argv) { new ServerChatUDP();}

    private void CriaJanela() {
        frame = new JFrame("Mensagem");
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLocation(frame.getToolkit().getScreenSize().width/2+frame.getToolkit().getScreenSize().width, frame.getToolkit().getScreenSize().height/2 - frame.getHeight()/2);
        frame.setVisible(true);
    }
    
    private DefaultTableModel iniciaJTable(JTable table) {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionModel(selectionModel);

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

        jPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        serverTextArea = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        clientesJTable = new javax.swing.JTable();

        serverTextArea.setColumns(20);
        serverTextArea.setRows(5);
        jScrollPane1.setViewportView(serverTextArea);

        clientesJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(clientesJTable);

        javax.swing.GroupLayout jPanelLayout = new javax.swing.GroupLayout(jPanel);
        jPanel.setLayout(jPanelLayout);
        jPanelLayout.setHorizontalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanelLayout.setVerticalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable clientesJTable;
    private javax.swing.JPanel jPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea serverTextArea;
    // End of variables declaration//GEN-END:variables
}
