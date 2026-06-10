import { KeyboardEvent } from "react";
import style from "./bookmarkCard.module.scss";

const TagInput = (props: {
  tags: readonly string[];
  inputValue: string;
  setInputValue: (val: string) => void;
  onDeleteTag: (index: string) => void;
  onPushTag: (tag: string) => void;
  testIdPrefix: `bk-${string}-` | `new-bk-`;
}) => {
  function onKeyDown(e: KeyboardEvent) {
    const { key } = e;
    const trimmedInput = props.inputValue.trim();
    if (
      // Add tag via space bar or enter
      (key === "Enter" || key === "Space" || key === " ") &&
      trimmedInput.length &&
      !props.tags.includes(trimmedInput)
    ) {
      e.preventDefault();
      props.onPushTag(props.inputValue);
      props.setInputValue("");
    }
    // backspace delete
    if (key === "Backspace" && !props.inputValue.length && props.tags.length) {
      e.preventDefault();
      const tagsCopy = [...props.tags];
      const poppedTag = tagsCopy.pop();
      if (poppedTag) {
        props.onDeleteTag(poppedTag);
        props.setInputValue(poppedTag);
      }
    }
  }

  return (
    <div className={style.container}>
      {props.tags.map((tag, index) => (
        <button
          key={tag}
          onClick={() => props.onDeleteTag(tag)}
          onContextMenu={(e) => {
            e.preventDefault();
            props.onDeleteTag(tag);
          }}
          type="button"
          className={style.pillButton}
          data-testid={`${props.testIdPrefix}tag-${tag}`}
        >
          {tag}
          <i className="xtag bi bi-journal-x"></i>
        </button>
      ))}

      <input
        className={style.input}
        value={props.inputValue}
        onChange={(e) => props.setInputValue(e.target.value)}
        onKeyDown={onKeyDown}
        placeholder="Enter a tag"
        data-testid={`${props.testIdPrefix}tag-input`}
      />
    </div>
  );
};

export default TagInput;
