import { ref } from 'vue'
import defaultAvatar from '@/assets/icons/profil.png'

export const user = ref({
  name: 'Navn navnesen',
  avatarUrl: defaultAvatar,
})
