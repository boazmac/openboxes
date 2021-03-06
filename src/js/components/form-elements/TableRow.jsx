import _ from 'lodash';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { renderFormField } from '../../utils/form-utils';

class TableRow extends Component {
  constructor(props) {
    super(props);

    this.state = {
      fieldPreview: _.isNil(props.fieldPreview) ? false : props.fieldPreview,
    };
  }

  componentWillReceiveProps(nextProps) {
    if (!nextProps.fieldPreview && this.state.fieldPreview) {
      this.setState({ fieldPreview: nextProps.fieldPreview });
    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (this.state.fieldPreview !== nextState.fieldPreview) {
      return true;
    }

    return !_.isEqualWith(_.omit(this.props, 'fieldPreview'), _.omit(nextProps, 'fieldPreview'), (objValue, othValue) => {
      if (typeof objValue === 'function' || typeof othValue === 'function') {
        return true;
      }

      return undefined;
    });
  }

  render() {
    const {
      fieldsConfig, index, field, addRow, properties, removeRow, rowValues = {},
    } = this.props;

    const dynamicAttr = fieldsConfig.getDynamicRowAttr ?
      fieldsConfig.getDynamicRowAttr({
        ...properties, index, rowValues, fieldPreview: this.state.fieldPreview,
      }) : {};
    const rowIndex = !_.isNil(properties.parentIndex) ? properties.parentIndex : index;
    const className = `table-row ${rowIndex % 2 === 0 ? 'even-row' : ''} ${dynamicAttr.className ? dynamicAttr.className : ''}`;

    return (
      <div {...dynamicAttr} className={className}>
        <div className="d-flex flex-row border-bottom table-inner-row">
          {_.map(fieldsConfig.fields, (config, name) => (
            <div key={`${field}.${name}`} className="align-self-center mx-1" style={{ flex: '1 1 0', minWidth: 0 }}>
              {renderFormField(config, `${field}.${name}`, {
                ...properties,
                fieldPreview: this.state.fieldPreview,
                arrayField: true,
                addRow,
                removeRow,
                rowIndex: index,
                fieldValue: config.fieldKey === '' ? rowValues : _.get(rowValues, config.fieldKey || name),
              })}
            </div>
          ))}
        </div>
      </div>);
  }
}

export default TableRow;

TableRow.propTypes = {
  fieldsConfig: PropTypes.shape({
    getDynamicAttr: PropTypes.func,
  }).isRequired,
  index: PropTypes.number.isRequired,
  field: PropTypes.string.isRequired,
  addRow: PropTypes.func.isRequired,
  removeRow: PropTypes.func.isRequired,
  properties: PropTypes.shape({}).isRequired,
  rowValues: PropTypes.shape({}),
  fieldPreview: PropTypes.bool,
};

TableRow.defaultProps = {
  rowValues: {},
  fieldPreview: false,
};
