import { useEffect, useRef } from 'react';
export default function ConfirmAttemptFinish({ onCancel, onConfirm }: { onCancel: () => void; onConfirm: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { const element = dialog.current; if (element?.showModal) element.showModal(); else element?.setAttribute('open', ''); }, []);
  return <dialog ref={dialog} aria-labelledby="confirm-attempt-title" onCancel={e => { e.preventDefault(); onCancel(); }}>
    <h3 id="confirm-attempt-title">Concluir simulado?</h3><p>Esta ação é irreversível. Questões em branco recebem zero. Confira as respostas confirmadas antes de continuar.</p>
    <button autoFocus onClick={onCancel}>Continuar respondendo</button><button onClick={onConfirm}>Confirmar conclusão</button>
  </dialog>;
}
