import AssistantWidget from './AssistantWidget.vue'

export default {
  install(app) {
    app.component('AIAssistant', AssistantWidget)
  }
}

export { AssistantWidget } 