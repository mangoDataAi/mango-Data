import { ref } from 'vue'

export function useLoading(initialState = false) {
  const loading = ref(initialState)

  const setLoading = (state) => {
    loading.value = state
  }

  const toggleLoading = () => {
    loading.value = !loading.value
  }

  return {
    loading,
    setLoading,
    toggleLoading
  }
}

export default useLoading 