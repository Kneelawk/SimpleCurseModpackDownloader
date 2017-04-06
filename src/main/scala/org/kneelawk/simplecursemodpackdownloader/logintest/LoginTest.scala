package org.kneelawk.simplecursemodpackdownloader.logintest

import org.kneelawk.simplecursemodpackdownloader.CurseUtils

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig

import dispatch.Defaults.executor
import dispatch.Http
import javafx.embed.swing.JFXPanel
import scalafx.Includes.handle
import scalafx.application.Platform
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.RowConstraints
import scalafx.stage.Stage

object LoginTest {
  val config = new AsyncHttpClientConfig.Builder()
    .setUserAgent("CurseModpackDownloader/0.0.1")
    .setRequestTimeout(-1)
    .setFollowRedirect(true)
    .build()
  val client = new Http(new AsyncHttpClient(config))

  def apply(args: Array[String]) {
    // initialize JavaFX
    new JFXPanel()

    Platform.runLater {
      val root = new GridPane {
        hgap = 5
        vgap = 5
        add(Label("Username:"), 0, 0)
        add(Label("Password:"), 0, 1)
        columnConstraints = Seq(
          new ColumnConstraints,
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints)
        rowConstraints = Seq(
          new RowConstraints,
          new RowConstraints,
          new RowConstraints { vgrow = Priority.Always },
          new RowConstraints)
      }

      val stage = new Stage {
        title = "Login Test"
        scene = new Scene(root)
        width = 400
        height = 150
      }

      val usernameField = new TextField()
      val passwordField = new PasswordField()
      val statusLabel = new Label("Not logged in")
      val exitButton = new Button {
        text = "Exit"
        onAction = handle(stage.close())
      }
      val loginButton = new Button {
        text = "Login"
      }

      def login = {
        statusLabel.text = "Logging in..."
        curseLogin(usernameField.getText, passwordField.getText, (success) =>
          Platform.runLater {
            if (success) statusLabel.text = "Login success"
            else statusLabel.text = "Login failure"
          })
      }

      usernameField.onAction = handle(login)
      passwordField.onAction = handle(login)
      loginButton.onAction = handle(login)

      root.add(usernameField, 1, 0)
      root.add(passwordField, 1, 1)
      root.add(statusLabel, 1, 3)
      root.add(new HBox(exitButton, loginButton) { spacing = 5 }, 2, 3)

      stage.showAndWait()

      client.shutdown()

      Platform.exit()
    }
  }

  def curseLogin(username: String, password: String, callback: (Boolean) => Unit) {
    val loginFut = CurseUtils.auth(client, username, password)
    for (data <- loginFut) {
      callback(true)
    }
    for (error <- loginFut.failed) {
      callback(false)
    }
  }
}