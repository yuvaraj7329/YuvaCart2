
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class YuvarajMart {

    public static void main(String[] args) throws Exception {

        // Changed port to 8081 to prevent port conflicts
        HttpServer server = HttpServer.create(
                new InetSocketAddress(8081), 0);

        server.createContext("/", YuvarajMart::handleRequest);

        server.start();

        System.out.println("================================");
        System.out.println("       YUVARAJ MART STARTED");
        System.out.println("================================");
        System.out.println("Open: http://localhost:8081");

        // Automatically open browser
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(
                        new URI("http://localhost:8081"));
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("Browser could not be opened automatically.");
        }
    }

    // Handle all requests
    static void handleRequest(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            showLoginPage(exchange);
        } else if (method.equalsIgnoreCase("POST")) {

            String data = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            String username = getValue(data, "username");
            String password = getValue(data, "password");

            if ("yuvaraj".equals(username) && "12345".equals(password)) {
                showShoppingPage(exchange, username);
            } else {
                showErrorPage(exchange);
            }
        } else {
            sendResponse(exchange, "Method not supported", 405);
        }
    }

    // ==============================
    // LOGIN PAGE
    // ==============================
    static void showLoginPage(HttpExchange exchange) throws IOException {

        String html = """
        <!DOCTYPE html>
        <html>
        <head>

        <title>Yuvaraj Mart - Login</title>

        <style>

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f1f5f9;
        }

        .header {
            background: linear-gradient(135deg, #6a11cb, #2575fc);
            color: white;
            text-align: center;
            padding: 35px;
        }

        .header h1 {
            margin: 0;
            font-size: 38px;
        }

        .header p {
            font-size: 17px;
        }

        .login-box {
            width: 360px;
            margin: 70px auto;
            padding: 35px;
            background: white;
            border-radius: 15px;
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
        }

        .login-box h2 {
            text-align: center;
            color: #333;
        }

        label {
            font-weight: bold;
            display: block;
            margin-top: 18px;
        }

        input {
            width: 100%;
            padding: 12px;
            margin-top: 7px;
            border: 1px solid #bbb;
            border-radius: 7px;
        }

        button {
            width: 100%;
            padding: 13px;
            margin-top: 25px;
            border: none;
            border-radius: 7px;
            background: #2575fc;
            color: white;
            font-size: 16px;
            cursor: pointer;
        }

        button:hover {
            background: #1258d8;
        }

        </style>
        </head>

        <body>

        <div class="header">
            <h1>🛒 Yuvaraj Mart</h1>
            <p>Online Shopping Website</p>
        </div>

        <div class="login-box">

            <h2>Login</h2>

            <form method="POST">

                <label>Username</label>
                <input
                    type="text"
                    name="username"
                    placeholder="Enter username"
                    required>

                <label>Password</label>
                <input
                    type="password"
                    name="password"
                    placeholder="Enter password"
                    required>

                <button type="submit">
                    Login
                </button>

            </form>

        </div>

        </body>
        </html>
        """;

        sendResponse(exchange, html, 200);
    }

    // ==============================
    // SHOPPING PAGE
    // ==============================
    static void showShoppingPage(
            HttpExchange exchange,
            String username) throws IOException {

        String html = """
        <!DOCTYPE html>
        <html>

        <head>

        <title>Yuvaraj Mart - Shopping</title>

        <style>

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f5f7fb;
        }

        .navbar {
            background: linear-gradient(135deg, #6a11cb, #2575fc);
            color: white;
            padding: 18px 40px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .navbar h1 {
            margin: 0;
        }

        .cart {
            background: white;
            color: #2575fc;
            padding: 10px 18px;
            border-radius: 20px;
            font-weight: bold;
        }

        .welcome {
            text-align: center;
            padding: 25px;
        }

        .welcome h2 {
            color: #333;
        }

        .products {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 25px;
            padding: 30px;
            max-width: 1200px;
            margin: auto;
        }

        .product {
            background: white;
            border-radius: 15px;
            padding: 20px;
            text-align: center;
            box-shadow: 0 5px 18px rgba(0,0,0,0.10);
            transition: 0.3s;
        }

        .product:hover {
            transform: translateY(-5px);
        }

        .product-icon {
            font-size: 70px;
            margin: 10px;
        }

        .product h3 {
            color: #333;
        }

        .price {
            color: #e63946;
            font-size: 20px;
            font-weight: bold;
        }

        .product button {
            background: #2575fc;
            color: white;
            border: none;
            padding: 11px 20px;
            border-radius: 7px;
            cursor: pointer;
            margin-top: 10px;
        }

        .product button:hover {
            background: #174db3;
        }

        .footer {
            margin-top: 40px;
            padding: 25px;
            background: #222;
            color: white;
            text-align: center;
        }

        </style>

        <script>

        let cartCount = 0;

        function addToCart(product) {
            cartCount++;
            document.getElementById("cart").innerHTML = "🛒 Cart (" + cartCount + ")";
            alert(product + " added to cart!");
        }

        </script>

        </head>

        <body>

        <div class="navbar">

            <h1>🛒 Yuvaraj Mart</h1>

            <div id="cart" class="cart">
                🛒 Cart (0)
            </div>

        </div>

        <div class="welcome">

            <h2>Welcome, %s! 👋</h2>

            <p>Explore our latest products</p>

        </div>

        <div class="products">

            <div class="product">
                <div class="product-icon">📱</div>
                <h3>Smart Phone</h3>
                <p>Latest Android Smartphone</p>
                <div class="price">₹18,999</div>
                <button onclick="addToCart('Smart Phone')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">💻</div>
                <h3>Laptop</h3>
                <p>Powerful Student Laptop</p>
                <div class="price">₹49,999</div>
                <button onclick="addToCart('Laptop')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">🎧</div>
                <h3>Headphones</h3>
                <p>Wireless Bluetooth Headphones</p>
                <div class="price">₹1,999</div>
                <button onclick="addToCart('Headphones')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">⌚</div>
                <h3>Smart Watch</h3>
                <p>Fitness Smart Watch</p>
                <div class="price">₹2,999</div>
                <button onclick="addToCart('Smart Watch')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">👟</div>
                <h3>Sports Shoes</h3>
                <p>Comfortable Running Shoes</p>
                <div class="price">₹1,499</div>
                <button onclick="addToCart('Sports Shoes')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">🎒</div>
                <h3>College Bag</h3>
                <p>Stylish Student Backpack</p>
                <div class="price">₹999</div>
                <button onclick="addToCart('College Bag')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">📷</div>
                <h3>Camera</h3>
                <p>Digital Camera</p>
                <div class="price">₹29,999</div>
                <button onclick="addToCart('Camera')">Add to Cart</button>
            </div>

            <div class="product">
                <div class="product-icon">🖥️</div>
                <h3>Monitor</h3>
                <p>Full HD Computer Monitor</p>
                <div class="price">₹9,999</div>
                <button onclick="addToCart('Monitor')">Add to Cart</button>
            </div>

        </div>

        <div class="footer">
            <h3>Yuvaraj Mart</h3>
            <p>Your favourite online shopping website</p>
            <p>© 2026 Yuvaraj Mart</p>
        </div>

        </body>
        </html>
        """.formatted(username);

        sendResponse(exchange, html, 200);
    }

    // ==============================
    // WRONG LOGIN
    // ==============================
    static void showErrorPage(HttpExchange exchange) throws IOException {

        String html = """
        <!DOCTYPE html>
        <html>

        <head>

        <title>Login Failed</title>

        <style>

        body {
            font-family: Arial;
            background: #ffe5e5;
            text-align: center;
            padding-top: 100px;
        }

        .box {
            background: white;
            width: 400px;
            margin: auto;
            padding: 40px;
            border-radius: 15px;
            box-shadow: 0 5px 20px #aaa;
        }

        h1 {
            color: red;
        }

        a {
            display: inline-block;
            margin-top: 20px;
            padding: 12px 25px;
            background: #2575fc;
            color: white;
            text-decoration: none;
            border-radius: 7px;
        }

        </style>

        </head>

        <body>

        <div class="box">

            <h1>❌ Login Failed</h1>

            <p>Username or password is incorrect.</p>

            <a href="/">
                Try Again
            </a>

        </div>

        </body>

        </html>
        """;

        sendResponse(exchange, html, 401);
    }

    // ==============================
    // GET FORM VALUE
    // ==============================
    static String getValue(String data, String key) {

        try {
            for (String pair : data.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2 && parts[0].equals(key)) {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to decode form data: " + e.getMessage());
        }

        return "";
    }

    // ==============================
    // SEND RESPONSE
    // ==============================
    static void sendResponse(
            HttpExchange exchange,
            String html,
            int statusCode) throws IOException {

        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8");

        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
