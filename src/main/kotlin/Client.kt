import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import kotlin.random.Random

class ChatApp : Application() {
    private lateinit var writer: PrintWriter
    private lateinit var reader: BufferedReader
    private lateinit var messageArea: VBox
    private lateinit var userList: ListView<String>
    private lateinit var username: String

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun start(stage: Stage) {
        messageArea = VBox(10.0).apply {
            styleClass.add("message-area")
        }

        val scrollPane = ScrollPane(messageArea).apply {
            isFitToWidth = true
            styleClass.add("text-area")
            vvalueProperty().bind(messageArea.heightProperty())
        }

        val inputField = TextField().apply {
            promptText = "Enter your message..."
            styleClass.add("text-field")
            setOnAction {
                val message = text
                scope.launch {
                    sendMessage(message)
                    Platform.runLater {
                        addMessage("You: $message", true)
                    }
                }
                clear()
            }
        }

        val sendButton = Button("Send").apply {
            styleClass.add("button")
            setOnAction {
                val message = inputField.text
                scope.launch {
                    sendMessage(message)
                    Platform.runLater {
                        addMessage("You: $message", true)
                    }
                }
                inputField.clear()
            }
        }

        val inputContainer = HBox(10.0, inputField, sendButton).apply {
            HBox.setHgrow(inputField, Priority.ALWAYS)
        }

        userList = ListView<String>().apply {
            styleClass.add("user-list")
            setOnMouseClicked { event ->
                if (event.clickCount == 1) {
                    val selectedUser = selectionModel.selectedItem
                    if (selectedUser != null) {
                        inputField.text = "/pm $selectedUser "
                        inputField.requestFocus()
                        inputField.positionCaret(inputField.text.length)
                    }
                }
            }
        }


        val sidebar = VBox(userList).apply {
            prefWidth = 200.0
            styleClass.add("sidebar")
        }

        val chatContainer = VBox(10.0, scrollPane, inputContainer).apply {
            VBox.setVgrow(scrollPane, Priority.ALWAYS)
        }

        val root = HBox(sidebar, chatContainer).apply {
            HBox.setHgrow(chatContainer, Priority.ALWAYS)
        }

        val scene = Scene(root, 800.0, 400.0)
        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())

        stage.title = "ChatApp"
        stage.scene = scene
        stage.show()

        connectToServer("127.0.0.1", 12345)
    }


    private fun connectToServer(host: String, port: Int) {
        scope.launch {
            val socket = Socket(host, port)
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            username = generateUsername()
            writer.println(username)

            reader.lines().forEach { message ->
                Platform.runLater {
                    addMessage(message, false)
                }
            }
        }
    }

        private suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            if (message.startsWith("/pm")) {
                val parts = message.split(" ", limit = 3)
                if (parts.size < 3) {
                    Platform.runLater {
                        addMessage("Usage: /pm [username] [message]", true)
                    }
                    return@withContext
                }
                val targetUser = parts[1]
                val actualMessage = parts[2]
                writer.println("PM|$targetUser|$username|$actualMessage")
            } else {
                writer.println("$username: $message")
            }
        }
    }

    private fun generateUsername(): String {
        return "User" + Random.nextInt(1000, 9999)
    }

    private fun addMessage(message: String, isUser: Boolean) {
        when {
            message.startsWith("UserList|") -> {
                val users = message.split("|").drop(1)
                Platform.runLater {
                    userList.items.clear()
                    userList.items.add("Вы: $username")
                    userList.items.addAll(users.filter { it != username })
                    userList.setCellFactory {
                        object : ListCell<String>() {
                            override fun updateItem(item: String?, empty: Boolean) {
                                super.updateItem(item, empty)
                                text = item ?: ""
                                if (item != null && item.startsWith("Вы: ")) {
                                    styleClass.add("you")
                                } else {
                                    styleClass.remove("you")
                                }
                            }
                        }
                    }
                }
            }
            message.startsWith("Private|") -> {
                val parts = message.split("|", limit = 3)
                if (parts.size == 3) {
                    val fromUser = parts[1]
                    val actualMessage = parts[2]
                    Platform.runLater {
                        val formattedMessage = "$fromUser (private): $actualMessage"
                        addTextMessage(formattedMessage, isUser = false, isPrivate = true)
                    }
                }
            }
            else -> {
                Platform.runLater {
                    addTextMessage(message, isUser, isPrivate = false)
                }
            }
        }
    }

    private fun addTextMessage(message: String, isUser: Boolean, isPrivate: Boolean) {
        val messageLabel = Label(message).apply {
            styleClass.add("message")
            if (isUser) {
                styleClass.add("you")
            } else if (isPrivate) {
                styleClass.add("private")
            }
            isWrapText = true
        }

        val messageContainer = HBox(messageLabel).apply {
            styleClass.add("message-container")
            if (isUser) {
                styleClass.add("you")
            } else if (isPrivate) {
                styleClass.add("private")
            }
            messageLabel.maxWidthProperty().bind(this.widthProperty().multiply(0.7))
        }

        messageArea.children.add(messageContainer)
    }
}

    fun main() {
        Application.launch(ChatApp::class.java)
    }