import { useState } from 'react'
import type { Difficulty, QuestionRequest, QuestionResponse } from '../../api/types'

type QuestionFormDialogProps = {
  initial?: QuestionResponse | null
  topicOptions: string[]
  onSave: (payload: QuestionRequest) => Promise<void>
  onCancel: () => void
}

export function QuestionFormDialog({ initial, topicOptions, onSave, onCancel }: QuestionFormDialogProps) {
  const [topicName, setTopicName] = useState(initial?.topicName ?? '')
  const [subTopic, setSubTopic] = useState(initial?.subTopic ?? '')
  const [title, setTitle] = useState(initial?.title ?? '')
  const [difficulty, setDifficulty] = useState<Difficulty>(initial?.difficulty ?? 'EASY')
  const [important, setImportant] = useState(initial?.important ?? false)
  const [practiceLink, setPracticeLink] = useState(initial?.practiceLink ?? '')
  const [youtubeLink, setYoutubeLink] = useState(initial?.youtubeLink ?? '')
  const [articleLink, setArticleLink] = useState(initial?.articleLink ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!topicName.trim() || !title.trim()) {
      setError('Topic and title are required.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave({
        topicName: topicName.trim(),
        subTopic: subTopic.trim() || undefined,
        title: title.trim(),
        difficulty,
        important,
        practiceLink: practiceLink.trim() || undefined,
        youtubeLink: youtubeLink.trim() || undefined,
        articleLink: articleLink.trim() || undefined,
      })
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to save question.')
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onMouseDown={onCancel}>
      <div className="modal-dialog" style={{ maxWidth: 480 }} onMouseDown={(e) => e.stopPropagation()}>
        <h3>{initial ? 'Edit question' : 'Add question'}</h3>
        <form onSubmit={handleSubmit}>
          {error && <div className="banner banner-error mt-1">{error}</div>}

          <div className="field mt-1">
            <label htmlFor="q-topic">Topic</label>
            <input
              id="q-topic"
              type="text"
              list="dsa-topic-options"
              value={topicName}
              onChange={(e) => setTopicName(e.target.value)}
              autoFocus
            />
            <datalist id="dsa-topic-options">
              {topicOptions.map((t) => (
                <option key={t} value={t} />
              ))}
            </datalist>
          </div>

          <div className="field">
            <label htmlFor="q-subtopic">Pattern / sub-topic (optional)</label>
            <input id="q-subtopic" type="text" value={subTopic} onChange={(e) => setSubTopic(e.target.value)} />
          </div>

          <div className="field">
            <label htmlFor="q-title">Question title</label>
            <input id="q-title" type="text" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>

          <div className="field">
            <label htmlFor="q-difficulty">Difficulty</label>
            <select id="q-difficulty" value={difficulty} onChange={(e) => setDifficulty(e.target.value as Difficulty)}>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
            </select>
          </div>

          <div className="checkbox-row">
            <input
              id="q-important"
              type="checkbox"
              checked={important}
              onChange={(e) => setImportant(e.target.checked)}
            />
            <label htmlFor="q-important" style={{ margin: 0 }}>
              Mark as important
            </label>
          </div>

          <div className="field mt-1">
            <label htmlFor="q-practice">Practice link (LeetCode/GfG/etc.)</label>
            <input
              id="q-practice"
              type="url"
              value={practiceLink}
              onChange={(e) => setPracticeLink(e.target.value)}
              placeholder="https://leetcode.com/problems/..."
            />
          </div>

          <div className="field">
            <label htmlFor="q-youtube">YouTube resource (optional)</label>
            <input id="q-youtube" type="url" value={youtubeLink} onChange={(e) => setYoutubeLink(e.target.value)} />
          </div>

          <div className="field">
            <label htmlFor="q-article">Article resource (optional)</label>
            <input id="q-article" type="url" value={articleLink} onChange={(e) => setArticleLink(e.target.value)} />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
