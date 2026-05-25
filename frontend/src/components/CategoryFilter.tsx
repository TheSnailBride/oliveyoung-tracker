import {
  ALL_CATEGORY_LABEL,
  CATEGORY_GROUPS,
  type CategoryGroup,
} from '../constants/categories';

interface CategoryFilterProps {
  category: string;
  selectedGroup?: CategoryGroup;
  onGroupChange: (groupName: string) => void;
  onCategoryChange: (categoryName: string) => void;
}

function CategoryFilter({
  category,
  selectedGroup,
  onGroupChange,
  onCategoryChange,
}: CategoryFilterProps) {
  return (
    <>
      <div className="category-group-list">
        <button
          onClick={() => onCategoryChange(ALL_CATEGORY_LABEL)}
          className={`category-chip category-group-chip ${category === ALL_CATEGORY_LABEL && !selectedGroup ? 'active' : ''}`}
        >
          {ALL_CATEGORY_LABEL}
        </button>
        {CATEGORY_GROUPS.map(group => (
          <button
            key={group.name}
            onClick={() => onGroupChange(group.name)}
            className={`category-chip category-group-chip ${selectedGroup?.name === group.name ? 'active' : ''}`}
          >
            {group.name}
          </button>
        ))}
      </div>

      {selectedGroup && (
        <div className="category-sub-list">
          {selectedGroup.items.map(item => (
            <button
              key={item}
              onClick={() => onCategoryChange(item)}
              className={`category-chip ${category === item ? 'active' : ''}`}
            >
              {item}
            </button>
          ))}
        </div>
      )}
    </>
  );
}

export default CategoryFilter;
