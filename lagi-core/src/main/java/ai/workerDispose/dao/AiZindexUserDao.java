package ai.workerDispose.dao;

import ai.database.impl.MysqlAdapter;
import ai.workerDispose.pojo.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AiZindexUserDao extends MysqlAdapter {

    public List<General2> getQianyiAiNodeCandidate(int batchLimit,Integer minNodeId,Integer maxNodeId) {
        //String sql = "SELECT name  AS value,node_id AS id FROM ai_node_candidate anc WHERE anc.node_id BETWEEN ? AND ? ORDER BY anc.node_id LIMIT ?";
        String sql = "SELECT nid, node_id AS nodeId FROM ai.ai_node_candidate as anc "+
        " left join ai_unindex_node as aun on aun.sub_id_in_table = anc.node_id " +
        " WHERE anc.node_id BETWEEN ? AND ? AND sub_node_table_index = 74 ORDER BY anc.node_id LIMIT ?;";
        List<General2> list = select(General2.class, sql,minNodeId,maxNodeId, batchLimit);
        return list.size() > 0 && list != null ? list : new ArrayList<>();
    }

    /**
     * 查询
     * @return
     */
    public List<NodeValue> getNodeValue(int pageNumber, int pageSize) {
        int pageNumbers =(pageNumber - 1) * pageSize;
        String sql = "SELECT azu.did,aud.plain_text AS plainText,azu.nid,aun.comments AS node,azu.weight " +
                " FROM (SELECT * FROM ai_zindex_user ORDER BY did LIMIT ?,?) azu \n" +
                " LEFT JOIN ai_unindex_dict aud ON aud.did = azu.did \n" +
                " LEFT JOIN ai_unindex_node aun ON aun.nid = azu.nid;";
        List<NodeValue> list = select(NodeValue.class, sql,pageNumbers, pageSize);
        return list.size() > 0 && list != null ? list : new ArrayList<>();
    }

    /**
     * 节点分类查询
     * @return
     */
    public List<ClassifyTxt> getClassifyTxt(String nids) {
        nids = nids.replace('，', ',');
        nids = nids.replace(" ", "");
        String[] nidsArray = nids.split(",");

        String qut = "SELECT " +
                "    u.sub_node_table_index," +
                "    s.sub_table_name AS name," +
                "    GROUP_CONCAT(u.nid ORDER BY u.nid) AS value" +
                " FROM ai_unindex_node u " +
                " LEFT JOIN ai_subindex_node s  ON u.sub_node_table_index = s.sub_id  " +
                " WHERE u.nid IN (" +
                Arrays.stream(nidsArray)
                        .map(id -> "'" + id + "'") // 为每个ID添加单引号
                        .collect(Collectors.joining(",")) + // 使用逗号连接
                ")" +
                "GROUP BY " +
                "    u.sub_node_table_index, s.sub_table_name;";
        List<General> generalList = select(General.class, qut);
        List<ClassifyTxt> list = new ArrayList<>();
        for (General general : generalList) {
            String value = general.getValue();
            String name = general.getName();
            value = value.replace('，', ',');
            value = value.replace(" ", "");
            String[] nidsArrayPlase = value.split(",");
            String sql = "SELECT nodes.*,anc.name AS node_text,anc.desc,anc.node_id FROM " +
                    " (SELECT aun.nid,aun.sub_id_in_table AS id ,aun.sub_node_table_index FROM " +
                    " ai_unindex_node aun WHERE nid IN (" +
                    Arrays.stream(nidsArrayPlase)
                            .map(id -> "'" + id + "'") // 为每个ID添加单引号
                            .collect(Collectors.joining(",")) + // 使用逗号连接
                    " ) ORDER BY aun.nid ) nodes " +
                    " LEFT JOIN "+name+" anc ON nodes.id = anc.node_id;";
            List<ClassifyTxt> list1 = select(ClassifyTxt.class, sql);
            list.addAll(list1);
        }

        return list.size() > 0 && list != null ? list : new ArrayList<>();
    }

    /**
     * 节点分类查询(分页版)
     * @param batchLimit
     * @param snti sub_node_table_index
     * @param minNid
     * @param maxNid
     * @return
     */
    public List<ClassifyTxt> getClassifyTxt(int batchLimit,int snti,Integer minNid,Integer maxNid) {
        String sql = "SELECT nodes.*,anc.name AS node_text,anc.desc,anc.node_id FROM " +
                "(SELECT aun.nid,aun.sub_id_in_table AS id ,aun.sub_node_table_index FROM " +
                " ai_unindex_node aun WHERE aun.sub_node_table_index = ? AND aun.nid BETWEEN ? AND ? ORDER BY aun.nid ) nodes\n" +
                "LEFT JOIN ai_node_candidate anc ON nodes.id = anc.node_id LIMIT ?;";

        List<ClassifyTxt> list = select(ClassifyTxt.class, sql,snti,minNid,maxNid, batchLimit);
        return list.size() > 0 && list != null ? list : new ArrayList<>();
    }

    public boolean getClassifyTxtCount(int snti,Integer minNid,Integer maxNid) {
        String sql = "SELECT count(*) FROM ai_unindex_node aun WHERE aun.sub_node_table_index = ? AND aun.nid BETWEEN ? AND ? ORDER BY aun.nid;";
        Integer msg = selectCount(sql,snti,minNid,maxNid);
        return msg > 0;
    }

    public boolean getQianyiAiNodeCandidateCount(Integer minNid,Integer maxNid) {
        String sql = "SELECT count(*) FROM ai.ai_node_candidate as anc "+
        " left join ai_unindex_node as aun on aun.sub_id_in_table = anc.node_id " +
        " WHERE anc.node_id BETWEEN ? AND ? AND sub_node_table_index = 74 ORDER BY anc.node_id;";
        Integer msg = selectCount(sql,minNid,maxNid);
        return msg > 0;
    }

    public List<DictValue> getDictList(int offset, int limit) {
        String sql = "SELECT azu.did,aud.plain_text AS plainText FROM " +
                "(SELECT distinct (did) FROM ai_zindex_user ORDER BY did limit ?, ?) azu\n" +
                " LEFT JOIN ai_unindex_dict aud ON aud.did = azu.did";
        List<DictValue> list = select(DictValue.class, sql, offset, limit);
        return list != null && !list.isEmpty() ? list : new ArrayList<>();
    }

    /**
     * 查询指数所有的类型
     * @return
     */
    public boolean updateWeight(Integer nid ,Integer did , Double weight) {
        int count =-1;
        Integer ret = -1;
        String sql1 = "SELECT COUNT(*) FROM ai_zindex_user1 WHERE nid = ? AND did = ? ";
        count = selectCount(sql1, nid, did);
        if (count > 0){
            String sql = "UPDATE ai_zindex_user1 " +
                    "SET weight = ? WHERE did = ? AND nid = ?;";
            ret =executeUpdate(sql,weight, did ,nid);
        }else {
            String sql = "INSERT INTO ai_zindex_user1(nid,did,weight) VALUES(?,?,?)";
            ret =executeUpdate(sql,nid, did ,weight);
        }

        return ret > 0;
    }
    /**
     * 查询权重
     * @return
     */
    public WeightObj selectWeight(Integer nid , Integer did) {
        String sql = "SELECT * FROM ai_zindex_user WHERE nid = ? AND did = ? ";
        List<WeightObj> list = select(WeightObj.class, sql,nid, did);
        if (list.size()> 0 && list != null){
            return list.get(0);
        }else {
            WeightObj weightObj = new WeightObj();
            weightObj.setDid(did);
            weightObj.setNid(nid);
            weightObj.setWeight(0.0);
            return weightObj;
        }
    }
}
