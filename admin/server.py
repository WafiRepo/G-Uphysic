import http.server
import socketserver
import logging
import datetime
import errno
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
    httpd = None
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

        base_port = int(os.environ.get("ADMIN_PORT", "8000"))
        # "" = semua interface (0.0.0.0); di Windows sering memicu WinError 10013 pada rentang port tertentu.
        bind_host = os.environ.get("ADMIN_HOST", "127.0.0.1").strip()
        if bind_host.lower() in ("*", "all"):
            bind_host = ""

        socketserver.TCPServer.allow_reuse_address = True

        chosen_port = None
        last_error = None
        for port in range(base_port, base_port + 30):
            try:
                httpd = socketserver.TCPServer((bind_host, port), Handler)
                chosen_port = port
                break
            except OSError as e:
                last_error = e
                win = getattr(e, "winerror", None)
                in_use = win == 10048 or e.errno == errno.EADDRINUSE
                forbidden = win == 10013 or isinstance(e, PermissionError)
                if in_use:
                    print(f"Port {port} sudah dipakai, mencoba {port + 1}...")
                    continue
                if forbidden and not bind_host:
                    print(
                        "Bind 0.0.0.0 ditolak (WinError 10013). "
                        "Jalankan dengan ADMIN_HOST=127.0.0.1 (default) atau coba port lain."
                    )
                    raise
                if forbidden:
                    print(f"Port {port} ditolak aksesnya (10013), mencoba {port + 1}...")
                    continue
                raise

        if httpd is None:
            logging.error(f"Tidak ada port kosong mulai {base_port}: {last_error}")
            raise last_error if last_error else RuntimeError("No port available")

        with httpd:
            url_host = bind_host if bind_host else "localhost"
            print(f"""
Server Started
-------------
URL: http://{url_host}:{chosen_port}
Time: {datetime.datetime.now()}
Directory: {os.getcwd()}
Bind: {bind_host or "0.0.0.0 (semua antarmuka)"}
Press Ctrl+C to stop
Lingkungan: ADMIN_PORT=8000  ADMIN_HOST=127.0.0.1  (kosongkan ADMIN_HOST untuk semua interface)
""")
            logging.info(f"Server started on {bind_host or '0.0.0.0'}:{chosen_port}")
            httpd.serve_forever()

    except KeyboardInterrupt:
        print("\nShutting down server...")
        logging.info("Server shutdown by user")
        try:
            httpd.shutdown()
        except Exception:
            pass
    except Exception as e:
        logging.error(f"Server error: {str(e)}", exc_info=True)
        raise

if __name__ == '__main__':
    run_server() 