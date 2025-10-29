/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient;

import penaltyclient.controller.LoginController;
/**
 *
 * @author This PC
 */
public class PenaltyClient {
    
    public static void main(String[] args) {
        LoginController loginController = new LoginController();
        loginController.showLoginView();
    }

}
