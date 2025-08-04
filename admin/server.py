import http.server
import socketserver
import logging
import datetime
import os
from urllib.parse import urlparse

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('server.log'),
        logging.StreamHandler()
    ]
)

PORT = 8000

class LoggingRequestHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        """Override the default logging to add more details"""
        logging.info(f"Client: {self.client_address[0]} - Request: {format%args}")

    def do_GET(self):
        """Handle GET requests with logging"""
        try:
            # Log request details
            parsed_path = urlparse(self.path)
            logging.info(f"""
Request Details:
---------------
Time: {datetime.datetime.now()}
Path: {parsed_path.path}
Client: {self.client_address[0]}
User-Agent: {self.headers.get('User-Agent')}
""")
            
            # Check if file exists
            if self.path == '/':
                self.path = '/index.html'
            
            file_path = os.path.join(os.getcwd(), self.path.lstrip('/'))
            if os.path.exists(file_path):
                logging.info(f"Serving file: {file_path}")
            else:
                logging.warning(f"File not found: {file_path}")

            return http.server.SimpleHTTPRequestHandler.do_GET(self)

        except Exception as e:
            logging.error(f"Error handling request: {str(e)}", exc_info=True)
            self.send_error(500, f"Internal server error: {str(e)}")

    def end_headers(self):
        """Add CORS headers"""
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()

def run_server():
    try:
        # Create logs directory if it doesn't exist
        if not os.path.exists('logs'):
            os.makedirs('logs')

        Handler = LoggingRequestHandler
        Handler.extensions_map.update({
            '.js': 'application/javascript',
            '.css': 'text/css',
            '.html': 'text/html',
            '.png': 'image/png',
            '.jpg': 'image/jpeg',
            '.svg': 'image/svg+xml',
        })

        with socketserver.TCPServer(("", PORT), Handler) as httpd:
            print(f"""
Server Started
-------------
URL: http://localhost:{PORT}
Time: {datetime.datetime.now()}
Directory: {os.getcwd()}
Press Ctrl+C to stop
""")
            logging.info(f"Server started on port {PORT}")
            httpd.serve_forever()

    except KeyboardInterrupt:
        print("\nShutting down server...")
        logging.info("Server shutdown by user")
        httpd.shutdown()
    except Exception as e:
        logging.error(f"Server error: {str(e)}", exc_info=True)
        raise

if __name__ == '__main__':
    run_server() 