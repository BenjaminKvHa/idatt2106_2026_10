# API Endpoints Guide

This document describes the backend endpoints currently set up in the project, how the frontend should call them, what they return, and how to handle the responses.

## Base URL and Auth

### Backend path vs frontend path

The backend controllers are mounted without `/api`, for example:

- backend route: `/auth/login`
- backend route: `/pupil/progress`

In the frontend, requests should go through nginx using the `/api` prefix:

- frontend call: `/api/auth/login`
- frontend call: `/api/pupil/progress`

This is handled by [frontend/nginx/default.conf](/Users/benjaminkvello-hansen/Projects/Syst%202/idatt2106_2026_10/frontend/nginx/default.conf).

### JWT and cookies

Most protected endpoints require:

- `Authorization: Bearer <accessToken>`
- `credentials: 'include'`

The frontend already has helpers for this in [frontend/src/utils/apiUtils.ts](/Users/benjaminkvello-hansen/Projects/Syst%202/idatt2106_2026_10/frontend/src/utils/apiUtils.ts).

Use:

- `loginWithCredentials(...)`
- `signupWithCredentials(...)`
- `get(...)`
- `post(...)`
- `put(...)`
- `patch(...)`
- `del(...)`

Example:

```ts
import { get } from '@/utils/apiUtils'

const data = await get('/api/pupil/progress')
```

### Common response handling

Most endpoints return one of these patterns:

- `200 OK` with JSON body
- `201 Created` with JSON body or primitive body
- `204 No Content`
- `401` when auth is missing/expired
- `403` when the user has the wrong role or no access
- `404` when the resource does not exist

The current wrapper throws:

```ts
throw new Error(`Request failed with status ${res.status}`)
```

So in the frontend, use `try/catch` around calls that can fail.

---

## Auth

These endpoints are used for sign-in, sign-up, refresh, and logout.

### `POST /api/auth/login`

Logs in a user and returns a JWT access token. Also sets the refresh-token cookie.

Request body:

```json
{
  "email": "teacher@example.com",
  "password": "Test1234"
}
```

Response:

```json
{
  "accessToken": "<jwt>"
}
```

Frontend example:

```ts
import { loginWithCredentials } from '@/utils/apiUtils'
import { decodeJwtPayload, getDefaultRouteForRole } from '@/utils/jwtUtils'

const accessToken = await loginWithCredentials({
  email: form.email,
  password: form.password,
})

const payload = decodeJwtPayload(accessToken)
router.push(getDefaultRouteForRole(payload?.role))
```

How to handle it:

- store the returned token in `sessionStorage`
- let the browser keep the refresh cookie
- decode the JWT only for frontend routing and UI behavior

### `POST /api/auth/signup`

Registers a new account and immediately logs the user in.

Request body:

```json
{
  "email": "pupil@example.com",
  "password": "Test1234",
  "legalName": "Pupil Name",
  "role": "PUPIL"
}
```

Allowed role values:

- `PUPIL`
- `TEACHER`

Response:

```json
{
  "accessToken": "<jwt>"
}
```

Frontend example:

```ts
import { signupWithCredentials } from '@/utils/apiUtils'

const accessToken = await signupWithCredentials({
  email: form.email,
  password: form.password,
  legalName: form.legalName,
  role: selectedRole,
})
```

How to handle it:

- same as login
- the frontend can keep role labels as `Lærer` and `Elev`, but must convert them before sending:
  - `Lærer -> TEACHER`
  - `Elev -> PUPIL`

### `POST /api/auth/refresh`

Refreshes the access token and rotates the refresh cookie.

Response:

```json
{
  "accessToken": "<jwt>"
}
```

Frontend example:

```ts
import { getNewTokens } from '@/utils/apiUtils'

await getNewTokens()
```

How to handle it:

- use the existing helper
- do not build manual refresh logic in each page

### `POST /api/auth/logout`

Logs the user out and expires the refresh cookie.

Response:

- `200 OK`
- empty body

Frontend example:

```ts
await fetch('/api/auth/logout', {
  method: 'POST',
  credentials: 'include',
  headers: {
    Authorization: `Bearer ${getAccessToken()}`,
  },
})

clearAuthSessionStorage()
router.push('/sign-in')
```

How to handle it:

- clear `sessionStorage`
- redirect to sign-in

---

## Teacher

These endpoints are for teacher-only pages and flows.

### `GET /api/teacher/classrooms`

Returns all classrooms the authenticated teacher belongs to.

Response:

```json
[
  {
    "classroomCode": "ABC123",
    "classroomName": "8A",
    "totalScore": 420,
    "globalRank": 2
  },
  {
    "classroomCode": "XYZ789",
    "classroomName": "9B",
    "totalScore": 210,
    "globalRank": 5
  }
]
```

Frontend example:

```ts
import { get } from '@/utils/apiUtils'

type TeacherClassroomOverview = {
  classroomCode: string
  classroomName: string
  totalScore: number
  globalRank: number
}

const classrooms = await get<TeacherClassroomOverview[]>('/api/teacher/classrooms')
```

How to handle it:

- call once on the teacher home/classroom overview page
- render one card or row per classroom
- use `classroomCode` for later classroom-specific requests

### `POST /api/classrooms`

Creates a new classroom.

Request body:

```json
{
  "name": "8A",
  "description": "Mandag formiddag"
}
```

Response:

```json
{
  "classroomCode": "ABC123",
  "classroomName": "8A",
  "classroomDescription": "Mandag formiddag"
}
```

Frontend example:

```ts
import { post } from '@/utils/apiUtils'

const classroom = await post('/api/classrooms', {
  name: form.name,
  description: form.description,
})
```

How to handle it:

- show the returned classroom code to the teacher
- update the overview list or refetch `/api/teacher/classrooms`

### `POST /api/classrooms/{classroomCode}/teachers`

Invites another teacher to the classroom.

Request body:

```json
{
  "email": "other.teacher@example.com"
}
```

Response:

- `200 OK`
- empty body

Frontend example:

```ts
await post(`/api/classrooms/${classroomCode}/teachers`, {
  email: inviteEmail,
})
```

How to handle it:

- show success message on `200`
- show backend error in `catch`

### `PUT /api/classrooms/{classroomCode}`

Updates classroom name and description.

Request body:

```json
{
  "name": "8A Oppdatert",
  "description": "Ny beskrivelse"
}
```

Response:

```json
{
  "classroomCode": "ABC123",
  "classroomName": "8A Oppdatert",
  "classroomDescription": "Ny beskrivelse"
}
```

Frontend example:

```ts
const updated = await put(`/api/classrooms/${classroomCode}`, {
  name: form.name,
  description: form.description,
})
```

### `DELETE /api/classrooms/{classroomCode}`

Deletes the classroom.

Response:

- `204 No Content`

Frontend example:

```ts
await del(`/api/classrooms/${classroomCode}`)
```

How to handle it:

- remove the classroom from the current UI list
- or refetch the teacher classroom overview

### `GET /api/classrooms/{classroomCode}/teachers/me/can-leave`

Checks whether the current teacher can leave the classroom.

Response:

```json
{
  "canLeave": true
}
```

Frontend example:

```ts
const status = await get<{ canLeave: boolean }>(
  `/api/classrooms/${classroomCode}/teachers/me/can-leave`,
)
```

### `DELETE /api/classrooms/{classroomCode}/teachers/me`

Removes the authenticated teacher from the classroom.

Response:

- `204 No Content`

Frontend example:

```ts
await del(`/api/classrooms/${classroomCode}/teachers/me`)
```

### `GET /api/classrooms/{classroomCode}/pupils/pending`

Returns pupils waiting for teacher approval.

Response:

```json
[
  {
    "accountEmail": "pupil@example.com",
    "legalName": "Pupil Name"
  }
]
```

Frontend example:

```ts
type PendingPupil = {
  accountEmail: string
  legalName: string
}

const pending = await get<PendingPupil[]>(
  `/api/classrooms/${classroomCode}/pupils/pending`,
)
```

### `GET /api/classrooms/{classroomCode}/pupils`

Returns all accepted pupils for one classroom. Only teachers in that classroom can use it.

Response:

```json
[
  {
    "accountEmail": "pupil1@example.com",
    "username": "detektiv1",
    "avatarObjectKey": "avatar-images/boy_hair1_costume1.png"
  },
  {
    "accountEmail": "pupil2@example.com",
    "username": "detektiv2",
    "avatarObjectKey": null
  }
]
```

Frontend example:

```ts
type AcceptedClassroomPupil = {
  accountEmail: string
  username: string | null
  avatarObjectKey: string | null
}

const pupils = await get<AcceptedClassroomPupil[]>(
  `/api/classrooms/${classroomCode}/pupils`,
)
```

How to handle it:

- use `accountEmail` as the stable pupil id for later teacher requests
- use `username` for display in the classroom member list
- if `avatarObjectKey` exists, extract the file name and build the image URL:
  - `avatar-images/boy_hair1_costume1.png`
  - becomes `/api/avatar-images/boy_hair1_costume1.png`
- if `avatarObjectKey` is `null`, show a fallback avatar

Frontend mapping example:

```ts
function getAvatarImageUrl(avatarObjectKey: string | null): string | null {
  if (!avatarObjectKey) {
    return null
  }

  const fileName = avatarObjectKey.split('/').pop()
  return fileName ? `/api/avatar-images/${fileName}` : null
}
```

### `PUT /api/classrooms/{classroomCode}/pupils/{pupilEmail}/accept`

Accepts a pupil into the classroom.

Response:

```json
{
  "accountEmail": "pupil@example.com",
  "legalName": "Pupil Name",
  "membershipConfirmedByTeacher": true
}
```

Frontend example:

```ts
await put(`/api/classrooms/${classroomCode}/pupils/${encodeURIComponent(pupilEmail)}/accept`, {})
```

Note:

- this endpoint uses `PUT` without a meaningful request body
- with the current wrapper, send `{}` as a simple empty JSON body

### `DELETE /api/classrooms/{classroomCode}/pupils/{pupilEmail}`

Kicks a pupil out of the classroom.

Response:

- `204 No Content`

Frontend example:

```ts
await del(`/api/classrooms/${classroomCode}/pupils/${encodeURIComponent(pupilEmail)}`)
```

### `GET /api/classrooms/{classroomCode}/leaderboard`

Returns the pupil leaderboard for one classroom.

Response:

```json
{
  "classroomCode": "ABC123",
  "classroomName": "8A",
  "entries": [
    {
      "username": "detektiv1",
      "totalXp": 240
    },
    {
      "username": "detektiv2",
      "totalXp": 180
    }
  ]
}
```

Frontend example:

```ts
type ClassroomLeaderboard = {
  classroomCode: string
  classroomName: string
  entries: Array<{
    username: string
    totalXp: number
  }>
}

const leaderboard = await get<ClassroomLeaderboard>(
  `/api/classrooms/${classroomCode}/leaderboard`,
)
```

### `GET /api/teacher/classrooms/{classroomCode}/pupils/{pupilEmail}/chapters/{chapterId}/notebook`

Lets a teacher view a pupil notebook for a chapter. Teacher must belong to the classroom.

Response:

```json
{
  "chapterId": 2,
  "tips": [
    "Se på overskriften først",
    "Sammenlign avsender og innhold"
  ],
  "ownNotes": "Pupilen skrev dette selv"
}
```

Frontend example:

```ts
type ChapterNotebook = {
  chapterId: number
  tips: string[]
  ownNotes: string
}

const notebook = await get<ChapterNotebook>(
  `/api/teacher/classrooms/${classroomCode}/pupils/${encodeURIComponent(pupilEmail)}/chapters/${chapterId}/notebook`,
)
```

How to handle it:

- render `tips` as read-only
- render `ownNotes` as read-only for teachers

---

## Pupil

These endpoints are for pupil pages and pupil-specific flows.

### `GET /api/pupil/progress`

Returns chapter progress for the authenticated pupil only.

Response:

```json
[
  {
    "chapterId": 1,
    "highscore": 3,
    "unlocked": true
  },
  {
    "chapterId": 2,
    "highscore": 1,
    "unlocked": true
  },
  {
    "chapterId": 3,
    "highscore": 0,
    "unlocked": false
  }
]
```

Frontend example:

```ts
type PupilChapterProgress = {
  chapterId: number
  highscore: number
  unlocked: boolean
}

const progress = await get<PupilChapterProgress[]>('/api/pupil/progress')
```

How to handle it:

- use `chapterId` to match frontend stops or chapters
- use `highscore` as the current number of stars
- use `unlocked` to enable or block access

### `GET /api/pupil/classrooms/{classroomCode}`

Returns pupil info for one classroom.

Response:

```json
{
  "email": "pupil@example.com",
  "username": "detektiv1",
  "legalName": "Pupil Name",
  "avatarFileObjectId": 42,
  "classroomCode": "ABC123",
  "membershipConfirmedByTeacher": true,
  "chapterProgressions": [
    {
      "chapterId": 1,
      "successfulRuns": 3,
      "highscore": 3,
      "unsuccessfulStreak": 0
    }
  ]
}
```

Frontend example:

```ts
const pupil = await get(`/api/pupil/classrooms/${classroomCode}`)
```

### `GET /api/pupil/classrooms/{classroomCode}/progression`

Returns progression entries for one classroom membership.

Response:

```json
{
  "chapterProgressions": [
    {
      "chapterId": 1,
      "successfulRuns": 3,
      "highscore": 3,
      "unsuccessfulStreak": 0
    },
    {
      "chapterId": 2,
      "successfulRuns": 1,
      "highscore": 2,
      "unsuccessfulStreak": 0
    }
  ]
}
```

Frontend example:

```ts
const progression = await get(`/api/pupil/classrooms/${classroomCode}/progression`)
```

### `GET /api/pupil/classrooms/{classroomCode}/membership`

Checks whether the authenticated pupil is a member of the classroom.

Response:

- `200 OK` if member
- `403 Forbidden` if not member
- empty body

Frontend example with raw `fetch`:

```ts
const res = await fetch(`/api/pupil/classrooms/${classroomCode}/membership`, {
  credentials: 'include',
  headers: {
    Authorization: `Bearer ${getAccessToken()}`,
  },
})

const isMember = res.ok
```

Why raw `fetch` can be useful here:

- the generic wrapper throws on `403`
- for this endpoint you may want a plain boolean instead

### `POST /api/pupil/classrooms/{classroomCode}/join`

Requests to join a classroom.

Response:

```json
{
  "email": "pupil@example.com",
  "username": null,
  "legalName": "Pupil Name",
  "avatarFileObjectId": null,
  "classroomCode": "ABC123",
  "membershipConfirmedByTeacher": false,
  "chapterProgressions": []
}
```

Frontend example:

```ts
const membership = await post(`/api/pupil/classrooms/${classroomCode}/join`, {})
```

Note:

- this endpoint does not need a real request body
- with the current wrapper, send `{}` as an empty JSON body

How to handle it:

- if successful, move the pupil to the next onboarding step
- do not assume access is fully granted until `membershipConfirmedByTeacher` is `true`

### `PUT /api/pupil/classrooms/{classroomCode}/membership`

Sets the pupil classroom membership details, including username and avatar selection.

Request body:

```json
{
  "username": "detektiv1",
  "avatar": "boy_hair1_costume2"
}
```

Important:

- `avatar` must have the format `<person>_<hair>_<costume>`
- example: `boy_hair1_costume2`

Response:

```json
{
  "username": "detektiv1",
  "avatarFileObjectId": 42,
  "membershipConfirmedByTeacher": false
}
```

Frontend example:

```ts
const updatedMembership = await put(
  `/api/pupil/classrooms/${classroomCode}/membership`,
  {
    username: form.username,
    avatar: `${person}_${hair}_${costume}`,
  },
)
```

### `GET /api/pupil/chapters/{chapterId}/notebook`

Returns the pupil’s own notebook for a chapter.

Response:

```json
{
  "chapterId": 1,
  "tips": [
    "Sjekk avsenderen",
    "Les hele meldingen nøye"
  ],
  "ownNotes": "Dette må jeg huske"
}
```

Frontend example:

```ts
const notebook = await get<{
  chapterId: number
  tips: string[]
  ownNotes: string
}>(`/api/pupil/chapters/${chapterId}/notebook`)
```

How to handle it:

- render `tips` as read-only saved tips
- render `ownNotes` in an editable textarea

### `PUT /api/pupil/chapters/{chapterId}/notebook`

Updates only the pupil’s own notes for a chapter.

Request body:

```json
{
  "ownNotes": "Ny tekst i notatboken"
}
```

Response:

```json
{
  "chapterId": 1,
  "tips": [
    "Sjekk avsenderen"
  ],
  "ownNotes": "Ny tekst i notatboken"
}
```

Frontend example:

```ts
const updated = await put(`/api/pupil/chapters/${chapterId}/notebook`, {
  ownNotes: notes,
})
```

How to handle it:

- save on explicit button click or debounced autosave
- replace local notebook state with the returned object

---

## Shared Leaderboards

These endpoints can be used in shared authenticated views.

### `GET /api/leaderboards/classrooms`

Returns the global classroom leaderboard.

Response:

```json
[
  {
    "classroomCode": "ABC123",
    "classroomName": "8A",
    "totalXp": 420
  },
  {
    "classroomCode": "XYZ789",
    "classroomName": "9B",
    "totalXp": 210
  }
]
```

Frontend example:

```ts
const leaderboard = await get<
  Array<{
    classroomCode: string
    classroomName: string
    totalXp: number
  }>
>('/api/leaderboards/classrooms')
```

How to handle it:

- render the array directly
- ranking numbers can be generated on the frontend from array order if needed

---

## Avatar Assets

These endpoints are useful for the avatar builder.

### `GET /api/avatar-assets`

Returns available avatar assets.

Response:

```json
[
  {
    "assetType": "hair",
    "name": "hair1",
    "filetype": "png"
  },
  {
    "assetType": "costume",
    "name": "costume2",
    "filetype": "png"
  }
]
```

Frontend example:

```ts
const assets = await get<
  Array<{
    assetType: string
    name: string
    filetype: string
  }>
>('/api/avatar-assets')
```

How to handle it:

- group by `assetType`
- build asset URLs using the next endpoint

### `GET /api/avatar-assets/{assetType}/{fileName}`

Returns the actual asset file bytes.

Examples:

- `/api/avatar-assets/hair/hair1.png`
- `/api/avatar-assets/costume/costume2.png`

Frontend usage:

- use this directly as an `<img>` source

Example:

```ts
const src = `/api/avatar-assets/hair/hair1.png`
```

```html
<img :src="src" alt="Hair option" />
```

### `GET /api/avatar-images/{fileName}`

Returns a saved generated avatar image as a PNG. This endpoint is public, but it only serves files that:

- exist in `file_object`
- are under the `avatar-images/` object-key prefix
- are marked `PUBLIC`

Examples:

- `/api/avatar-images/boy_hair1_costume1.png`
- `/api/avatar-images/girl_hair2_costume1.png`

Frontend usage:

- use it directly as the `<img>` source after extracting the file name from `avatarObjectKey`

Example:

```ts
const avatarObjectKey = 'avatar-images/boy_hair1_costume1.png'
const fileName = avatarObjectKey.split('/').pop()
const src = fileName ? `/api/avatar-images/${fileName}` : null
```

```html
<img v-if="src" :src="src" alt="Avatar" />
```

How to handle it:

- this endpoint does not need JWT in the browser
- it is only for saved generated avatar images, not arbitrary files
- do not call `/api/objects/{id}` for avatar display lists unless you specifically need protected file access

---

## File Objects

These endpoints are for uploaded files and generated avatar images.

### `POST /api/objects`

Uploads a file.

Request:

- `multipart/form-data`

Fields:

- `file`: the uploaded file
- `classroomCode`: optional
- `accessLevel`: optional, default is `PUBLIC`

Allowed `accessLevel` values:

- `PUBLIC`
- `ONLY_CLASS`
- `ONLY_TEACHERS_IN_CLASS`

Response:

- `201 Created`
- body is a number with the new file object id

Example response:

```json
42
```

Frontend example:

```ts
import { getAccessToken } from '@/utils/apiUtils'

const formData = new FormData()
formData.append('file', selectedFile)
formData.append('classroomCode', classroomCode)
formData.append('accessLevel', 'ONLY_CLASS')

const res = await fetch('/api/objects', {
  method: 'POST',
  credentials: 'include',
  headers: {
    Authorization: `Bearer ${getAccessToken()}`,
  },
  body: formData,
})

if (!res.ok) {
  throw new Error(`Request failed with status ${res.status}`)
}

const fileObjectId = await res.json()
```

Why raw `fetch` is recommended here:

- this endpoint uses `multipart/form-data`
- the current generic wrapper is JSON-oriented

### `GET /api/objects/{fileObjectId}`

Returns the file bytes if the current user has access.

Frontend usage:

- use this directly as an image or download URL if the browser has the right auth context
- or fetch it manually if needed

Example:

```ts
const imageUrl = `/api/objects/${fileObjectId}`
```

### `DELETE /api/objects/{fileObjectId}`

Deletes a file object the current user is allowed to delete.

Response:

- `204 No Content`

Frontend example:

```ts
await del(`/api/objects/${fileObjectId}`)
```

---

## Recommended Frontend Patterns

### 1. Use typed calls

Prefer defining local TypeScript types per page:

```ts
type TeacherClassroomOverview = {
  classroomCode: string
  classroomName: string
  totalScore: number
  globalRank: number
}

const classrooms = await get<TeacherClassroomOverview[]>('/api/teacher/classrooms')
```

### 2. Use `try/catch` around page loads and form submits

```ts
loading.value = true
errorMessage.value = ''

try {
  classrooms.value = await get<TeacherClassroomOverview[]>('/api/teacher/classrooms')
} catch (error) {
  errorMessage.value = error instanceof Error ? error.message : 'Noe gikk galt'
} finally {
  loading.value = false
}
```

### 3. Keep pupil identity out of pupil-owned requests

For pupil endpoints, do not send:

- pupil email
- account id
- username as identity

The backend already resolves the pupil from the JWT.

### 4. Use returned IDs and codes for navigation

Important fields to keep:

- `classroomCode` for classroom-specific pages
- `chapterId` for chapter-specific pages
- `avatarFileObjectId` and `fileObjectId` for file/image rendering

### 5. Render read-only vs editable fields correctly

Notebook handling:

- `tips`: read-only
- `ownNotes`: editable for pupils only

Teacher notebook view:

- everything should be read-only

---

## Quick Endpoint Index

### Public/Auth

- `POST /api/auth/login`
- `POST /api/auth/signup`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### Teacher

- `GET /api/teacher/classrooms`
- `POST /api/classrooms`
- `POST /api/classrooms/{classroomCode}/teachers`
- `PUT /api/classrooms/{classroomCode}`
- `DELETE /api/classrooms/{classroomCode}`
- `GET /api/classrooms/{classroomCode}/teachers/me/can-leave`
- `DELETE /api/classrooms/{classroomCode}/teachers/me`
- `GET /api/classrooms/{classroomCode}/pupils/pending`
- `GET /api/classrooms/{classroomCode}/pupils`
- `PUT /api/classrooms/{classroomCode}/pupils/{pupilEmail}/accept`
- `DELETE /api/classrooms/{classroomCode}/pupils/{pupilEmail}`
- `GET /api/classrooms/{classroomCode}/leaderboard`
- `GET /api/teacher/classrooms/{classroomCode}/pupils/{pupilEmail}/chapters/{chapterId}/notebook`

### Pupil

- `GET /api/pupil/progress`
- `GET /api/pupil/classrooms/{classroomCode}`
- `GET /api/pupil/classrooms/{classroomCode}/progression`
- `GET /api/pupil/classrooms/{classroomCode}/membership`
- `POST /api/pupil/classrooms/{classroomCode}/join`
- `PUT /api/pupil/classrooms/{classroomCode}/membership`
- `GET /api/pupil/chapters/{chapterId}/notebook`
- `PUT /api/pupil/chapters/{chapterId}/notebook`

### Shared

- `GET /api/leaderboards/classrooms`
- `GET /api/avatar-assets`
- `GET /api/avatar-assets/{assetType}/{fileName}`
- `GET /api/avatar-images/{fileName}`
- `POST /api/objects`
- `GET /api/objects/{fileObjectId}`
- `DELETE /api/objects/{fileObjectId}`
