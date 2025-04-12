# Fibonacci Retry Manager 🔁

A robust retry manager built with a Fibonacci backoff strategy. Useful for scenarios where transient failures may occur, such as network requests or database operations.

---

## 📌 Features

- ✅ Supports `Runnable` and `Callable` tasks.
- 📈 Fibonacci-based delay between retries.
- 🧠 Configurable retry policy.
- 🛠️ Hooks for custom retry event listeners.
- 📦 Clean logging with SLF4J.
- 📄 Well-documented with JavaDocs.

---

## 🚀 Getting Started

### 📥 Installation

Clone the repo:

```bash
git clone https://github.com/NOURELHODABARKAT/fibonacci-backoff-manager.git
cd fibonacci-backoff-manager
Make sure you have Java 11+ installed
🛠️ Usage
Runnable Example
RetryPolicy policy = new FibonacciRetryPolicy(5);
RetryListener listener = new SimpleRetryListener();

FibonacciRetryManager manager = new FibonacciRetryManager(policy, listener);

manager.executeWithRetry(() -> {
    // Task logic that might fail
});
Callable Example
java
Copy
Edit
String result = manager.executeWithRetry(() -> {
    // Task logic that returns a result
    return "Done!";
});
You can run tests using
mvn test
# or
gradle test
🤝 Contributing
Pull requests are welcome!
For major changes, please open an issue first to discuss what you would like to change
✨ Author
A backend developer passionate about resilient and smart systems 🤍
