/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import convertors.ConvertToJson;
import entity.Product;
import entity.Purchase;
import entity.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
import java.util.List;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import session.ProductFacade;
import session.PurchaseFacade;
import session.UserFacade;
import tools.PassEncrypt;

/**
 *
 * @author pupil
 */
@WebServlet(name = "UserServlet", urlPatterns = {
    "/userRegistration",
    "/listUsers",
    "/createPurchase",
    "/addMoney"
})
public class UserServlet extends HttpServlet {
    public static enum Role {USER,EMPLOYEE,ADMINISTRATOR};
    private PassEncrypt passEncrypt;
    @EJB UserFacade userFacade;
    @EJB ProductFacade productFacade;
    @EJB PurchaseFacade purchaseFacade;
   
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        JsonObjectBuilder job = Json.createObjectBuilder();
         HttpSession session = request.getSession(false);
        if(session == null){
            job.add("info", "Login please!");
            try (PrintWriter out = response.getWriter()) {
                out.println(job.build().toString());
            }
            return;
        }
        User authUser = (User) session.getAttribute("authUser");
        if(authUser == null){
            job.add("info", "Login please!");
            try (PrintWriter out = response.getWriter()) {
                out.println(job.build().toString());
            }
            return;
        }
        if(!authUser.getRoles().contains(UserServlet.Role.EMPLOYEE.toString())){
            job.add("info", "Login please!");
            try (PrintWriter out = response.getWriter()) {
                out.println(job.build().toString());
            }
            return;
        }
        String path = request.getServletPath();
        switch (path) {
            case "/userRegistration":
                JsonReader jsonReader = Json.createReader(request.getReader());
                JsonObject jsonObject = jsonReader.readObject();
                String firstname = jsonObject.getString("firstname");
                String lastname = jsonObject.getString("lastname");
                String phone = jsonObject.getString("phone");
                String login = jsonObject.getString("login");
                String password = jsonObject.getString("password");
 
                User user = new User();
                user.setFirstname(firstname);
                user.setLastname(lastname);
                user.setPhone(phone);
                user.setLogin(login);
                
                passEncrypt = new PassEncrypt();
                user.setSalt(passEncrypt.getSalt());
                password = passEncrypt.getEncryptedPass(password, user.getSalt());
                user.setPassword(password);
                user.getRoles().add(UserServlet.Role.USER.toString());
                userFacade.create(user);
                job.add("info", "User added");
                try (PrintWriter out = response.getWriter()) {
                    out.println(job.build().toString());
                }
              break;
            case "/listUsers":
                List<User> listUsers = userFacade.findAll();
                job = Json.createObjectBuilder();
                job.add("status", true);
                job.add("users", new ConvertToJson().getJsonArrayUsers(listUsers));
                
                
                try (PrintWriter out = response.getWriter()) {
                    out.println(job.build().toString());
                }
              break;
            case "/createPurchase":
                    jsonReader = Json.createReader(request.getReader());
                    jsonObject = jsonReader.readObject();
                    String purchaseId = jsonObject.getString("purchaseId");
                    String productId = jsonObject.getString("purchaseId");
                    String quantity = jsonObject.getString("quantity");
                    job = Json.createObjectBuilder(); 
                    Purchase purchase = new Purchase();
                    Product product = productFacade.find(Long.parseLong(productId));
                    if(authUser.getWallet() > (product.getCost()*Integer.parseInt(quantity)) && product.getQuantity()>= Integer.parseInt(quantity)){

                        purchase.setDate(new GregorianCalendar().getTime());
                        purchase.setHistory((product.getCost())*Integer.parseInt(quantity));
                        purchase.setQuantity(Integer.parseInt(quantity));
                        authUser.setWallet(authUser.getWallet() - ((product.getCost())*Integer.parseInt(quantity)));

                        product.setQuantity(product.getQuantity() - Integer.parseInt(quantity));
                        purchase.setProduct(product);
                        purchase.setUser(authUser);
                        userFacade.edit(authUser);
                        productFacade.edit(product);
                        purchaseFacade.create(purchase);
                    
                        job.add("info", "Purchased");
                        try (PrintWriter out = response.getWriter()) {
                            out.println(job.build().toString());
                        }
                    }else if(authUser.getWallet() < (product.getCost()*Integer.parseInt(quantity))){
                         job.add("info", "Not enough money");
                        try (PrintWriter out = response.getWriter()) {
                            out.println(job.build().toString());
                        }
                    }else if (product.getQuantity() < Integer.parseInt(quantity) && (product.getQuantity() > 0)){
                         job.add("info", "We dont have such amount of this product");
                        try (PrintWriter out = response.getWriter()) {
                            out.println(job.build().toString());
                        }
                    }else if (product.getQuantity() == 0){
                         job.add("info", "We dont have this porudct in stock");
                        try (PrintWriter out = response.getWriter()) {
                            out.println(job.build().toString());
                        }
                }
                break;

            case "/addMoney":
                jsonReader = Json.createReader(request.getReader());
                jsonObject = jsonReader.readObject();
                String moneyQo = jsonObject.getString("moneyQo");                             
                authUser.setWallet(authUser.getWallet() + Integer.parseInt(moneyQo));   
                userFacade.edit(authUser);   
                job.add("info", "Added");
                    try (PrintWriter out = response.getWriter()) {
                        out.println(job.build().toString());
                    }    
               
                break;   
            
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
