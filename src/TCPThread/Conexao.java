/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPThread;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 *
 * @author felipesoares
 */
public class Conexao extends Thread{

    private final int cli;
    private final Socket cliente;
    private final JTextArea serverTextArea;


    public Conexao(Socket cliente, int cli, JTextArea serverTextArea){
        this.cliente = cliente;
        this.cli = cli;
        this.serverTextArea = serverTextArea;
    }


    @Override
    public void run() {

        Scanner resposta;
        String string;
        PrintStream ps;


        try {

            serverTextArea.setText(serverTextArea.getText()+"Client accepted\n");
            serverTextArea.setText(serverTextArea.getText()+"IP: "+ cliente.getInetAddress()+"\n");

            if(cliente.isClosed()){
                serverTextArea.setText(serverTextArea.getText()+"Conexão fechada\n");
                return;
            }


            resposta = new Scanner(cliente.getInputStream());



            ps = new PrintStream(cliente.getOutputStream());

            while(resposta.hasNextLine()){
                string = resposta.nextLine();
                serverTextArea.setText(serverTextArea.getText()+"Cliente "+cli+":\n"+string+"\n");
                ps.println("Cliente "+cli+":\n"+string.toUpperCase());        
            }
        } catch (IOException ex) {
            serverTextArea.setText(serverTextArea.getText()+"Erro no processamento da mensagem\n");
        } finally {
            try{
                cliente.close();
                serverTextArea.setText(serverTextArea.getText()+"Conexão fechada com o cliente"+ cli+"\n");
            } catch(Exception e) {
                serverTextArea.setText(serverTextArea.getText()+"Erro ao fechar a conexão com o cliente "+ cli+"\n");

            }

        }

    }  

}
