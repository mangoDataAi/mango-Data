import AssistantWidget from './AssistantWidget.vue'

export default {
  install(app) {
    app.component('SmartAssistant', AssistantWidget)
  }
}

export { AssistantWidget } 